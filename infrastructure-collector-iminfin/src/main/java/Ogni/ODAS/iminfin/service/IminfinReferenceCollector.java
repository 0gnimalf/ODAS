package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.util.IminfinPeriodFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IminfinReferenceCollector implements ExternalReferenceCollectorPort {

    private static final String DEFAULT_TERRITORY_PARAMETER = "territory";
    private static final String TERRITORY_DATA_SOURCE = "TerritoryOnlySubject";
    private static final String TERRITORY_PERIOD_PARAMETER = "TERRITORIES_paramPeriod";
    private static final String TERRITORY_PERIOD_VALUE = "2014-05-28T00:00:00.000Z";
    private static final String OUTCOME_TYPES_DATA_SOURCE = "PassportFK_002_002_outcomesTypesFix";
    private static final String CREDIT_PARAMETERS_DATA_SOURCE = "PassportFK_001_005_paramCreditsData_fixed";
    private static final List<IndicatorGroupCode> SUPPORTED_GROUPS = List.of(
            IndicatorGroupCode.INCOME,
            IndicatorGroupCode.OUTCOME,
            IndicatorGroupCode.CREDIT,
            IndicatorGroupCode.FIN_SOURCE
    );

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
    public List<CollectedRegionDto> collectRegions() {
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

    @Override
    public List<CollectedIndicatorDto> collectIndicators(SyncIndicatorsCommand command) {
        Objects.requireNonNull(command, "Indicators command must not be null");
        Objects.requireNonNull(command.year(), "Indicators year must not be null");

        String requestedPeriod = IminfinPeriodFormatter.format(command.year(), 1);
        if (command.groupCode() != null) {
            return collectIndicators(command.groupCode(), requestedPeriod);
        }

        List<CollectedIndicatorDto> result = new ArrayList<>();
        for (IndicatorGroupCode groupCode : SUPPORTED_GROUPS) {
            result.addAll(collectIndicators(groupCode, requestedPeriod));
        }
        return result;
    }

    private List<CollectedIndicatorDto> collectIndicators(IndicatorGroupCode groupCode, String requestedPeriod) {
        return switch (groupCode) {
            case INCOME -> collectIncomeIndicators(requestedPeriod);
            case OUTCOME -> collectOutcomeIndicators(requestedPeriod);
            case CREDIT -> collectCreditIndicators();
            case FIN_SOURCE -> collectFinSourceIndicators(requestedPeriod);
            default -> throw new IllegalStateException("Unexpected indicator group code: " + groupCode);
        };
    }

    private List<CollectedIndicatorDto> collectIncomeIndicators(String requestedPeriod) {
        return collectTreeIndicators(
                IminfinPassportPage.INCOMES_DETAIL,
                IndicatorGroupCode.INCOME,
                "income",
                null,
                requestedPeriod
        );
    }

    private List<CollectedIndicatorDto> collectOutcomeIndicators(String requestedPeriod) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        IminfinDataSourceDefinition types = reportDefinition.requireDataSource(OUTCOME_TYPES_DATA_SOURCE);
        List<CollectedIndicatorDto> result = new ArrayList<>();

        for (List<String> row : types.fixedData()) {
            OutcomeTypeSpec spec = toOutcomeTypeSpec(row);
            if (spec == null) {
                continue;
            }
            result.addAll(collectTreeIndicators(
                    reportDefinition,
                    IndicatorGroupCode.OUTCOME,
                    spec.prefix(),
                    spec.outcomesType(),
                    requestedPeriod
            ));
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

    private List<CollectedIndicatorDto> collectFinSourceIndicators(String requestedPeriod) {
        return collectTreeIndicators(
                IminfinPassportPage.FIN_SOURCES_DETAIL,
                IndicatorGroupCode.FIN_SOURCE,
                "fin-source",
                null,
                requestedPeriod
        );
    }

    private List<CollectedIndicatorDto> collectTreeIndicators(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String rootPrefix,
            Integer outcomesType,
            String requestedPeriod
    ) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(page);
        return collectTreeIndicators(reportDefinition, groupCode, rootPrefix, outcomesType, requestedPeriod);
    }

    private List<CollectedIndicatorDto> collectTreeIndicators(
            IminfinReportDefinition reportDefinition,
            IndicatorGroupCode groupCode,
            String rootPrefix,
            Integer outcomesType,
            String requestedPeriod
    ) {
        String territoryCode = reportDefinition.defaultValue(DEFAULT_TERRITORY_PARAMETER);
        IminfinLoadedData loadedData = reportDataLoader.loadDetailData(
                reportDefinition,
                territoryCode,
                requestedPeriod,
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
