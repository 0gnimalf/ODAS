package Ogni.ODAS.iminfin.collector;

import Ogni.ODAS.application.dto.ExternalDatasetPayload;
import Ogni.ODAS.application.dto.ExternalObservationRow;
import Ogni.ODAS.application.dto.ExternalRegionRef;
import Ogni.ODAS.application.port.out.collector.ExternalPopulationCollectorPort;
import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.service.IminfinReportDataLoader;
import Ogni.ODAS.iminfin.service.IminfinReportDiscoveryService;
import Ogni.ODAS.iminfin.util.IminfinJsonTableHelper;
import Ogni.ODAS.iminfin.util.IminfinPeriodFormatter;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class IminfinPopulationCollector implements ExternalPopulationCollectorPort {

    public static final String POPULATION_INDICATOR_NAME = "Численность населения";

    private static final String POPULATION_LABEL = "численность населения (чел.)";
    private static final String TERRITORY_PARAMETER = "territory";
    private static final String PERIOD_PARAMETER = "paramPeriod";
    private static final String NAME_COLUMN = "name";
    private static final String POPULATION_VALUE_COLUMN = "prevYearFact";

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinReportDataLoader reportDataLoader;

    public IminfinPopulationCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinReportDataLoader reportDataLoader
    ) {
        this.discoveryService = Objects.requireNonNull(discoveryService);
        this.reportDataLoader = Objects.requireNonNull(reportDataLoader);
    }

    @Override
    public List<ExternalDatasetPayload> collectPopulationObservations(
            int year,
            Collection<ExternalRegionRef> regions
    ) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }

        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.PASSPORT_ROOT);
        List<String> populationDataSourceCodes = resolvePopulationDataSourceCodes(report);
        if (populationDataSourceCodes.isEmpty()) {
            throw new IllegalStateException("Unable to resolve iMinfin population data source for report " + report.title());
        }

        String firstMonthPeriod = IminfinPeriodFormatter.format(year, 1);
        List<ExternalDatasetPayload> result = new ArrayList<>();
        for (ExternalRegionRef region : regions) {
            collectRegionPopulation(report, populationDataSourceCodes, region, firstMonthPeriod)
                    .ifPresent(result::add);
        }
        return result;
    }

    private Optional<ExternalDatasetPayload> collectRegionPopulation(
            IminfinReportDefinition report,
            List<String> populationDataSourceCodes,
            ExternalRegionRef region,
            String firstMonthPeriod
    ) {
        if (region == null || region.externalCode() == null || region.externalCode().isBlank()) {
            return Optional.empty();
        }

        for (String dataSourceCode : populationDataSourceCodes) {
            IminfinLoadedData loaded;
            try {
                loaded = reportDataLoader.loadData(
                        report,
                        dataSourceCode,
                        Map.of(
                                TERRITORY_PARAMETER, region.externalCode(),
                                PERIOD_PARAMETER, firstMonthPeriod
                        )
                );
            } catch (RuntimeException ignored) {
                continue;
            }

            Optional<BigDecimal> population = extractPopulationValue(loaded);
            if (population.isPresent()) {
                ExternalObservationRow row = new ExternalObservationRow(
                        SourceSystemCode.IMINFIN,
                        region.externalCode(),
                        IndicatorGroupCode.OTHER,
                        POPULATION_INDICATOR_NAME,
                        null,
                        ObservationValueKind.POPULATION,
                        population.get()
                );
                return Optional.of(toPayload(report, loaded, List.of(row)));
            }
        }
        return Optional.empty();
    }

    private List<String> resolvePopulationDataSourceCodes(IminfinReportDefinition report) {
        return report.dataSources().entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> isPopulationCandidate(entry.getValue()))
                .map(Map.Entry::getKey)
                .distinct()
                .toList();
    }

    private boolean isPopulationCandidate(IminfinDataSourceDefinition dataSource) {
        if (dataSource == null) {
            return false;
        }
        return dataSource.columnNames().contains(NAME_COLUMN)
                && dataSource.columnNames().contains(POPULATION_VALUE_COLUMN)
                && dataSource.parameters().contains(TERRITORY_PARAMETER)
                && dataSource.parameters().contains(PERIOD_PARAMETER);
    }

    private Optional<BigDecimal> extractPopulationValue(IminfinLoadedData loadedData) {
        if (loadedData == null || !loadedData.dataRows().isArray() || loadedData.dataRows().isEmpty()) {
            return Optional.empty();
        }

        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(loadedData.dataSource().columnNames());
        Integer nameIndex = columns.get(NAME_COLUMN);
        Integer populationIndex = columns.get(POPULATION_VALUE_COLUMN);

        for (JsonNode row : loadedData.dataRows()) {
            String label = IminfinJsonTableHelper.textCell(row, nameIndex);
            if (!POPULATION_LABEL.equals(TextNormalizer.normalize(label))) {
                continue;
            }

            BigDecimal value = IminfinJsonTableHelper.decimalCell(row, populationIndex);
            if (value != null && value.signum() > 0) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private ExternalDatasetPayload toPayload(
            IminfinReportDefinition report,
            IminfinLoadedData loaded,
            List<ExternalObservationRow> observations
    ) {
        OffsetDateTime externalDate = report.dataVersionDate() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : report.dataVersionDate();
        return new ExternalDatasetPayload(
                SourceSystemCode.IMINFIN,
                report.title() + " [" + report.page().name() + ":" + loaded.dataSourceCode() + ":population]",
                externalDate,
                loaded.request(),
                loaded.rawData(),
                observations
        );
    }
}
