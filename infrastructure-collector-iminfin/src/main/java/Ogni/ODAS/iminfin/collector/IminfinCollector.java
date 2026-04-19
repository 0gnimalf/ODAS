package Ogni.ODAS.iminfin.collector;

import Ogni.ODAS.application.dto.*;
import Ogni.ODAS.application.port.out.collector.ExternalIndicatorCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalObservationCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalRegionCollectorPort;
import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinParsedIndicatorRow;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.service.IminfinIndicatorTreeParser;
import Ogni.ODAS.iminfin.service.IminfinObservationMapper;
import Ogni.ODAS.iminfin.service.IminfinReportDataLoader;
import Ogni.ODAS.iminfin.service.IminfinReportDiscoveryService;
import Ogni.ODAS.iminfin.util.IminfinPeriodFormatter;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class IminfinCollector implements ExternalRegionCollectorPort, ExternalIndicatorCollectorPort, ExternalObservationCollectorPort {

    private static final String TERRITORY_DATA_SOURCE = "TerritoryOnlySubject";
    private static final String TERRITORY_PERIOD_PARAMETER = "TERRITORIES_paramPeriod";
    private static final String TERRITORY_PERIOD_VALUE = "2014-05-28T00:00:00.000Z";
    private static final String DEFAULT_TERRITORY_PARAMETER = "territory";
    private static final String OUTCOME_TYPES_DATA_SOURCE = "PassportFK_002_002_outcomesTypesFix";
    private static final String CREDIT_PARAMETERS_DATA_SOURCE = "PassportFK_001_005_paramCreditsData_fixed";
    private static final String CREDIT_DATA_SOURCE = "PassportFK_001_005_creditGridData";
    private static final String CREDIT_PARAMETER_NAME = "PassportFK_001_005_paramCredits";

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinReportDataLoader reportDataLoader;
    private final IminfinIndicatorTreeParser indicatorTreeParser;
    private final IminfinObservationMapper observationMapper;

    public IminfinCollector() {
        this(IminfinCollectorProperties.defaults());
    }

    public IminfinCollector(IminfinCollectorProperties properties) {
        IminfinHttpClient httpClient = new IminfinHttpClient(properties);
        this.discoveryService = new IminfinReportDiscoveryService(httpClient, properties);
        this.reportDataLoader = new IminfinReportDataLoader(discoveryService, httpClient);
        this.indicatorTreeParser = new IminfinIndicatorTreeParser();
        this.observationMapper = new IminfinObservationMapper();
    }

    @Override
    public List<ExternalRegionRow> collectRegions() {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.INCOMES_DETAIL);
        IminfinLoadedData loaded = reportDataLoader.loadData(
                report,
                TERRITORY_DATA_SOURCE,
                Map.of(TERRITORY_PERIOD_PARAMETER, TERRITORY_PERIOD_VALUE)
        );
        JsonNode data = loaded.dataRows();
        if (!data.isArray()) {
            throw new IllegalStateException("Unexpected territory response for report " + report.title());
        }
        Map<String, String> regions = new LinkedHashMap<>();
        for (JsonNode row : data) {
            if (!row.isArray() || row.size() < 2) {
                continue;
            }
            String externalCode = row.get(0).asText(null);
            String name = row.get(1).asText(null);
            if (externalCode != null && !externalCode.isBlank() && name != null && !name.isBlank()) {
                regions.putIfAbsent(externalCode, name);
            }
        }
        return regions.entrySet().stream()
                .map(entry -> new ExternalRegionRow(SourceSystemCode.IMINFIN, entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<ExternalIndicatorRow> collectIndicators(IndicatorGroupCode groupCode, int year) {
        String requestedPeriod = IminfinPeriodFormatter.format(year, 1);
        return switch (groupCode) {
            case INCOME ->
                    collectDetailIndicators(IminfinPassportPage.INCOMES_DETAIL, groupCode, "income", null, requestedPeriod);
            case OUTCOME -> collectOutcomeIndicators(requestedPeriod);
            case CREDIT -> collectCreditIndicators();
            case FIN_SOURCE ->
                    collectDetailIndicators(IminfinPassportPage.FIN_SOURCES_DETAIL, groupCode, "fin-source", null, requestedPeriod);
            default -> throw new IllegalStateException("Unsupported indicator group: " + groupCode);
        };
    }

    @Override
    public List<ExternalDatasetPayload> collectObservations(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            Collection<ExternalRegionRef> regions
    ) {
        String requestedPeriod = IminfinPeriodFormatter.format(year, month);
        return switch (groupCode) {
            case INCOME ->
                    collectDetailObservationPayloads(IminfinPassportPage.INCOMES_DETAIL, groupCode, "income", "Доходы", null, requestedPeriod, regions);
            case OUTCOME -> collectOutcomeObservationPayloads(requestedPeriod, regions);
            case CREDIT -> collectCreditObservationPayloads(requestedPeriod, regions);
            case FIN_SOURCE ->
                    collectDetailObservationPayloads(IminfinPassportPage.FIN_SOURCES_DETAIL, groupCode, "fin-source", "Источники финансирования", null, requestedPeriod, regions);
            default -> throw new IllegalStateException("Unsupported indicator group: " + groupCode);
        };
    }

    private List<ExternalIndicatorRow> collectOutcomeIndicators(String requestedPeriod) {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        List<ExternalIndicatorRow> result = new ArrayList<>();
        for (OutcomeTypeSpec spec : outcomeTypes(report)) {
            result.addAll(collectDetailIndicators(report, IndicatorGroupCode.OUTCOME, spec.namespace(), spec.value(), requestedPeriod));
        }
        return result;
    }

    private List<ExternalDatasetPayload> collectOutcomeObservationPayloads(String requestedPeriod, Collection<ExternalRegionRef> regions) {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        List<ExternalDatasetPayload> result = new ArrayList<>();
        for (OutcomeTypeSpec spec : outcomeTypes(report)) {
            result.addAll(collectDetailObservationPayloads(report, IndicatorGroupCode.OUTCOME, spec.namespace(), spec.displayPrefix(), spec.value(), requestedPeriod, regions));
        }
        return result;
    }

    private List<ExternalIndicatorRow> collectDetailIndicators(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String namespace,
            Integer outcomesType,
            String requestedPeriod
    ) {
        return collectDetailIndicators(discoveryService.discover(page), groupCode, namespace, outcomesType, requestedPeriod);
    }

    private List<ExternalIndicatorRow> collectDetailIndicators(
            IminfinReportDefinition report,
            IndicatorGroupCode groupCode,
            String namespace,
            Integer outcomesType,
            String requestedPeriod
    ) {
        String territoryCode = report.defaultValue(DEFAULT_TERRITORY_PARAMETER);
        IminfinLoadedData loaded = reportDataLoader.loadDetailData(report, territoryCode, requestedPeriod, outcomesType);
        return indicatorTreeParser.parseDetailRows(namespace, loaded.dataSource(), loaded.dataRows()).stream()
                .map(row -> new ExternalIndicatorRow(
                        groupCode,
                        row.naturalKey(),
                        row.name(),
                        row.parentNaturalKey(),
                        row.level(),
                        row.sortOrder(),
                        row.hasChildren()
                ))
                .toList();
    }

    private List<ExternalDatasetPayload> collectDetailObservationPayloads(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String namespace,
            String displayPrefix,
            Integer outcomesType,
            String requestedPeriod,
            Collection<ExternalRegionRef> regions
    ) {
        return collectDetailObservationPayloads(discoveryService.discover(page), groupCode, namespace, displayPrefix, outcomesType, requestedPeriod, regions);
    }

    private List<ExternalDatasetPayload> collectDetailObservationPayloads(
            IminfinReportDefinition report,
            IndicatorGroupCode groupCode,
            String namespace,
            String displayPrefix,
            Integer outcomesType,
            String requestedPeriod,
            Collection<ExternalRegionRef> regions
    ) {
        List<ExternalDatasetPayload> result = new ArrayList<>();
        for (ExternalRegionRef region : regions) {
            IminfinLoadedData loaded = reportDataLoader.loadDetailData(report, region.externalCode(), requestedPeriod, outcomesType);
            List<IminfinParsedIndicatorRow> parsedRows = indicatorTreeParser.parseDetailRows(namespace, loaded.dataSource(), loaded.dataRows());
            List<ExternalObservationRow> observations = observationMapper.mapDetailObservations(
                    region.externalCode(),
                    groupCode,
                    loaded.dataSource(),
                    parsedRows
            );
            result.add(toPayload(report, loaded, observations));
        }
        return result;
    }

    private List<ExternalIndicatorRow> collectCreditIndicators() {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        IminfinDataSourceDefinition source = report.requireDataSource(CREDIT_PARAMETERS_DATA_SOURCE);
        List<CreditIndicatorSpec> specs = creditIndicatorSpecs(source);
        Set<String> parentKeys = new HashSet<>();
        specs.stream().map(CreditIndicatorSpec::parentNaturalKey).filter(Objects::nonNull).forEach(parentKeys::add);
        return specs.stream()
                .map(spec -> new ExternalIndicatorRow(
                        IndicatorGroupCode.CREDIT,
                        spec.naturalKey(),
                        spec.name(),
                        spec.parentNaturalKey(),
                        spec.level(),
                        spec.sortOrder(),
                        parentKeys.contains(spec.naturalKey())
                ))
                .toList();
    }

    private List<ExternalDatasetPayload> collectCreditObservationPayloads(String requestedPeriod, Collection<ExternalRegionRef> regions) {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        IminfinDataSourceDefinition source = report.requireDataSource(CREDIT_PARAMETERS_DATA_SOURCE);
        Map<String, String> externalCodeByName = regions.stream()
                .collect(LinkedHashMap::new,
                        (map, region) -> map.putIfAbsent(TextNormalizer.normalize(region.name()), region.externalCode()),
                        Map::putAll);
        List<ExternalDatasetPayload> result = new ArrayList<>();
        for (CreditIndicatorSpec spec : creditIndicatorSpecs(source)) {
            IminfinLoadedData loaded = reportDataLoader.loadData(
                    report,
                    CREDIT_DATA_SOURCE,
                    Map.of(
                            CREDIT_PARAMETER_NAME, spec.sourceParameterValue(),
                            "paramPeriod", requestedPeriod
                    )
            );
            List<ExternalObservationRow> observations = observationMapper.mapCreditObservations(
                    spec.name(),
                    loaded.dataSource(),
                    loaded.dataRows(),
                    externalCodeByName
            );
            result.add(toPayload(report, loaded, observations));
        }
        return result;
    }

    private ExternalDatasetPayload toPayload(IminfinReportDefinition report, IminfinLoadedData loaded, List<ExternalObservationRow> observations) {
        OffsetDateTime externalDate = report.dataVersionDate() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : report.dataVersionDate();
        return new ExternalDatasetPayload(
                SourceSystemCode.IMINFIN,
                report.title() + " [" + report.page().name() + ":" + loaded.dataSourceCode() + "]",
                externalDate,
                loaded.request(),
                loaded.rawData(),
                observations
        );
    }

    private List<OutcomeTypeSpec> outcomeTypes(IminfinReportDefinition report) {
        IminfinDataSourceDefinition types = report.requireDataSource(OUTCOME_TYPES_DATA_SOURCE);
        List<OutcomeTypeSpec> result = new ArrayList<>();
        for (List<String> row : types.fixedData()) {
            if (row.size() < 2) {
                continue;
            }
            String sourceCode = row.get(1);
            if ("2".equals(sourceCode)) {
                result.add(new OutcomeTypeSpec(2, "outcome/rzpr", "Расходы (РЗПР)"));
            } else if ("3".equals(sourceCode)) {
                result.add(new OutcomeTypeSpec(3, "outcome/kvr", "Расходы (КВР)"));
            }
        }
        if (result.isEmpty()) {
            result.add(new OutcomeTypeSpec(2, "outcome/rzpr", "Расходы (РЗПР)"));
            result.add(new OutcomeTypeSpec(3, "outcome/kvr", "Расходы (КВР)"));
        }
        return result;
    }

    private List<CreditIndicatorSpec> creditIndicatorSpecs(IminfinDataSourceDefinition source) {
        List<CreditIndicatorSpec> result = new ArrayList<>();
        String currentParentKey = null;
        List<String> currentParentPath = List.of();
        int sortOrder = 0;
        Set<String> usedKeys = new HashSet<>();
        for (List<String> row : source.fixedData()) {
            if (row.size() < 2) {
                continue;
            }
            String caption = row.get(0);
            String parameterValue = row.get(1);
            if (caption == null || caption.isBlank() || parameterValue == null || parameterValue.isBlank()) {
                continue;
            }
            sortOrder++;
            boolean child = caption.trim().startsWith("-");
            String cleanCaption = child ? caption.trim().substring(1).trim() : caption.trim();
            String parentKey = child ? currentParentKey : null;
            List<String> path = new ArrayList<>();
            if (child) {
                path.addAll(currentParentPath);
            }
            path.add(cleanCaption);
            String baseKey = parentKey == null
                    ? "credit/" + TextNormalizer.slugify(cleanCaption)
                    : parentKey + "/" + TextNormalizer.slugify(cleanCaption);
            String key = uniqueKey(baseKey, usedKeys);
            if (!child) {
                currentParentKey = key;
                currentParentPath = List.of(cleanCaption);
            }
            result.add(new CreditIndicatorSpec(key, parentKey, cleanCaption, child ? 1 : 0, sortOrder, parameterValue));
        }
        return result;
    }

    private String uniqueKey(String baseKey, Set<String> usedKeys) {
        String key = baseKey;
        int duplicate = 2;
        while (!usedKeys.add(key)) {
            key = baseKey + "-" + duplicate;
            duplicate++;
        }
        return key;
    }

    private record OutcomeTypeSpec(int value, String namespace, String displayPrefix) {
    }

    private record CreditIndicatorSpec(
            String naturalKey,
            String parentNaturalKey,
            String name,
            int level,
            int sortOrder,
            String sourceParameterValue
    ) {
    }
}
