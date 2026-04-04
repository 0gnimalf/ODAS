package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedReferenceCatalogDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinReferenceCollector implements ExternalReferenceCollectorPort {

    private static final String DEFAULT_TERRITORY_PARAMETER = "territory";
    private static final String TERRITORY_DATA_SOURCE = "TerritoryOnlySubject";
    private static final String TERRITORY_PERIOD_PARAMETER = "TERRITORIES_paramPeriod";
    private static final String TERRITORY_PERIOD_VALUE = "2014-05-28T00:00:00.000Z";
    private static final String OUTCOME_TYPES_DATA_SOURCE = "PassportFK_002_002_outcomesTypesFix";
    private static final String CREDIT_PARAMETERS_DATA_SOURCE = "PassportFK_001_005_paramCreditsData_fixed";

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinReportDataLoader reportDataLoader;
    private final IminfinIndicatorTreeParser indicatorTreeParser;
    private final IminfinFederalDistrictResolver federalDistrictResolver;

    public IminfinReferenceCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinReportDataLoader reportDataLoader,
            IminfinIndicatorTreeParser indicatorTreeParser,
            IminfinFederalDistrictResolver federalDistrictResolver
    ) {
        this.discoveryService = discoveryService;
        this.reportDataLoader = reportDataLoader;
        this.indicatorTreeParser = indicatorTreeParser;
        this.federalDistrictResolver = federalDistrictResolver;
    }

    @Override
    public CollectedReferenceCatalogDto collectReferenceCatalog() {
        List<CollectedRegionDto> regions = collectRegions();
        List<CollectedIndicatorDto> indicators = new ArrayList<>();
        indicators.addAll(collectIncomeIndicators());
        indicators.addAll(collectOutcomeIndicators());
        indicators.addAll(collectCreditIndicators());
        indicators.addAll(collectFinSourceIndicators());
        return new CollectedReferenceCatalogDto(regions, indicators);
    }

    private List<CollectedRegionDto> collectRegions() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.INCOMES_DETAIL);
        IminfinLoadedData loadedData = reportDataLoader.loadData(
                reportDefinition,
                TERRITORY_DATA_SOURCE,
                Map.of(TERRITORY_PERIOD_PARAMETER, TERRITORY_PERIOD_VALUE)
        );

        JsonNode data = loadedData.dataRows();
        if (!data.isArray()) {
            throw new IllegalStateException("Unexpected territory response for report " + reportDefinition.title());
        }

        Map<String, String> regions = new LinkedHashMap<>();
        for (JsonNode row : data) {
            if (!row.isArray() || row.size() < 2) {
                continue;
            }
            regions.put(row.get(0).asText(), row.get(1).asText());
        }

        return regions.entrySet().stream()
                .map(entry -> new CollectedRegionDto(
                        entry.getKey(),
                        entry.getValue(),
                        federalDistrictResolver.resolve(entry.getValue())
                ))
                .toList();
    }

    private List<CollectedIndicatorDto> collectIncomeIndicators() {
        return collectTreeIndicators(
                IminfinPassportPage.INCOMES_DETAIL,
                IndicatorGroupCode.INCOME,
                "income",
                null
        );
    }

    private List<CollectedIndicatorDto> collectOutcomeIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        IminfinDataSourceDefinition types = reportDefinition.requireDataSource(OUTCOME_TYPES_DATA_SOURCE);
        List<CollectedIndicatorDto> result = new ArrayList<>();

        for (List<String> row : types.fixedData()) {
            OutcomeTypeSpec spec = toOutcomeTypeSpec(row);
            if (spec == null) {
                continue;
            }
            result.addAll(collectTreeIndicators(reportDefinition, IndicatorGroupCode.OUTCOME, spec.prefix(), spec.outcomesType()));
        }

        return result;
    }

    private List<CollectedIndicatorDto> collectCreditIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        IminfinDataSourceDefinition dataSource = reportDefinition.requireDataSource(CREDIT_PARAMETERS_DATA_SOURCE);
        List<CollectedIndicatorDto> result = new ArrayList<>();
        String currentParentCode = null;
        int sortOrder = 0;

        for (List<String> row : dataSource.fixedData()) {
            if (row.size() < 2) {
                continue;
            }

            String caption = row.get(0);
            String sourceCode = row.get(1);
            String code = "credit/" + sourceCode;
            boolean child = caption != null && caption.trim().startsWith("-");
            String cleanCaption = child ? caption.trim().substring(1).trim() : caption.trim();
            if (!child) {
                currentParentCode = code;
            }
            sortOrder++;

            result.add(new CollectedIndicatorDto(
                    code,
                    cleanCaption,
                    IndicatorGroupCode.CREDIT,
                    child ? currentParentCode : null,
                    child ? 2 : 1,
                    sortOrder,
                    false
            ));
        }
        return result;
    }

    private List<CollectedIndicatorDto> collectFinSourceIndicators() {
        return collectTreeIndicators(
                IminfinPassportPage.FIN_SOURCES_DETAIL,
                IndicatorGroupCode.FIN_SOURCE,
                "fin-source",
                null
        );
    }

    private List<CollectedIndicatorDto> collectTreeIndicators(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String rootPrefix,
            Integer outcomesType
    ) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(page);
        return collectTreeIndicators(reportDefinition, groupCode, rootPrefix, outcomesType);
    }

    private List<CollectedIndicatorDto> collectTreeIndicators(
            IminfinReportDefinition reportDefinition,
            IndicatorGroupCode groupCode,
            String rootPrefix,
            Integer outcomesType
    ) {
        String period = discoveryService.loadLatestPeriod(reportDefinition);
        String territoryCode = reportDefinition.defaultValue(DEFAULT_TERRITORY_PARAMETER);
        IminfinLoadedData loadedData = reportDataLoader.loadDetailData(
                reportDefinition,
                territoryCode,
                period,
                outcomesType
        );

        return indicatorTreeParser.parseDetailRows(rootPrefix, loadedData.dataSource(), loadedData.dataRows()).stream()
                .map(parsed -> new CollectedIndicatorDto(
                        parsed.code(),
                        parsed.caption(),
                        groupCode,
                        parsed.parentCode(),
                        parsed.level(),
                        parsed.sortOrder(),
                        parsed.section()
                ))
                .toList();
    }

    private OutcomeTypeSpec toOutcomeTypeSpec(List<String> row) {
        if (row.size() < 2) {
            return null;
        }

        String sourceCode = row.get(1);
        return switch (sourceCode) {
            case "2" -> new OutcomeTypeSpec(2, "outcome/rzpr");
            case "3" -> new OutcomeTypeSpec(3, "outcome/kvr");
            default -> null;
        };
    }

    private record OutcomeTypeSpec(int outcomesType, String prefix) {
    }
}
