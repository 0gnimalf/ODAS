package Ogni.ODAS.iminfin.collector;

import Ogni.ODAS.application.dto.*;
import Ogni.ODAS.application.port.out.collector.ExternalIndicatorCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalObservationCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalPopulationCollectorPort;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class IminfinCollector implements ExternalRegionCollectorPort, ExternalIndicatorCollectorPort, ExternalObservationCollectorPort, ExternalPopulationCollectorPort {

    private static final String TERRITORY_DATA_SOURCE = "TerritoryOnlySubject";
    private static final String TERRITORY_PERIOD_PARAMETER = "TERRITORIES_paramPeriod";
    private static final String TERRITORY_PERIOD_VALUE = "2014-05-28T00:00:00.000Z";
    private static final String OUTCOME_TYPES_DATA_SOURCE = "PassportFK_002_002_outcomesTypesFix";
    private static final String CREDIT_PARAMETERS_DATA_SOURCE = "PassportFK_001_005_paramCreditsData_fixed";
    private static final String CREDIT_DATA_SOURCE = "PassportFK_001_005_creditGridData";
    private static final String CREDIT_PARAMETER_NAME = "PassportFK_001_005_paramCredits";

    private final IminfinCollectorProperties properties;
    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinReportDataLoader reportDataLoader;
    private final IminfinIndicatorTreeParser indicatorTreeParser;
    private final IminfinObservationMapper observationMapper;
    private final IminfinPopulationCollector populationCollector;

    public IminfinCollector() {
        this(IminfinCollectorProperties.defaults());
    }

    public IminfinCollector(IminfinCollectorProperties properties) {
        this.properties = properties == null ? IminfinCollectorProperties.defaults() : properties;
        IminfinHttpClient httpClient = new IminfinHttpClient(this.properties);
        this.discoveryService = new IminfinReportDiscoveryService(httpClient, this.properties);
        this.reportDataLoader = new IminfinReportDataLoader(discoveryService, httpClient);
        this.indicatorTreeParser = new IminfinIndicatorTreeParser();
        this.observationMapper = new IminfinObservationMapper();
        this.populationCollector = new IminfinPopulationCollector(this.discoveryService, this.reportDataLoader);
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
            case INCOME -> collectMergedDetailIndicators(
                    IminfinPassportPage.INCOMES_DETAIL,
                    groupCode,
                    "income",
                    null,
                    null,
                    requestedPeriod,
                    true
            );
            case OUTCOME -> collectOutcomeIndicators(requestedPeriod);
            case CREDIT -> collectCreditIndicators();
            case FIN_SOURCE -> collectMergedDetailIndicators(
                    IminfinPassportPage.FIN_SOURCES_DETAIL,
                    groupCode,
                    "fin-source",
                    null,
                    null,
                    requestedPeriod,
                    false
            );
            case OTHER -> collectPopulationIndicators();
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
            case INCOME -> collectDetailObservationPayloads(
                    IminfinPassportPage.INCOMES_DETAIL,
                    groupCode,
                    "income",
                    null,
                    null,
                    requestedPeriod,
                    regions,
                    true
            );
            case OUTCOME -> collectOutcomeObservationPayloads(requestedPeriod, regions);
            case CREDIT -> collectCreditObservationPayloads(requestedPeriod, regions);
            case FIN_SOURCE -> collectDetailObservationPayloads(
                    IminfinPassportPage.FIN_SOURCES_DETAIL,
                    groupCode,
                    "fin-source",
                    null,
                    null,
                    requestedPeriod,
                    regions,
                    false
            );
            case OTHER -> List.of();
        };
    }

    @Override
    public List<ExternalDatasetPayload> collectPopulationObservations(
            int year,
            Collection<ExternalRegionRef> regions
    ) {
        return populationCollector.collectPopulationObservations(year, regions);
    }


    private List<ExternalIndicatorRow> collectPopulationIndicators() {
        return List.of(new ExternalIndicatorRow(
                IndicatorGroupCode.OTHER,
                "population",
                IminfinPopulationCollector.POPULATION_INDICATOR_NAME,
                null,
                0,
                1,
                false
        ));
    }

    private List<ExternalIndicatorRow> collectOutcomeIndicators(String requestedPeriod) {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        List<ExternalIndicatorRow> result = new ArrayList<>();
        for (OutcomeTypeSpec spec : outcomeTypes(report)) {
            result.addAll(collectMergedDetailIndicators(
                    report,
                    IndicatorGroupCode.OUTCOME,
                    spec.namespace(),
                    spec.serviceRootName(),
                    spec.value(),
                    requestedPeriod,
                    true
            ));
        }
        return result;
    }

    private List<ExternalDatasetPayload> collectOutcomeObservationPayloads(String requestedPeriod, Collection<ExternalRegionRef> regions) {
        IminfinReportDefinition report = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        List<ExternalDatasetPayload> result = new ArrayList<>();
        for (OutcomeTypeSpec spec : outcomeTypes(report)) {
            result.addAll(collectDetailObservationPayloads(
                    report,
                    IndicatorGroupCode.OUTCOME,
                    spec.namespace(),
                    spec.serviceRootName(),
                    spec.value(),
                    requestedPeriod,
                    regions,
                    true
            ));
        }
        return result;
    }

    private List<ExternalIndicatorRow> collectMergedDetailIndicators(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String namespace,
            String serviceRootName,
            Integer outcomesType,
            String requestedPeriod,
            boolean useFirstRootAsAggregateParent
    ) {
        return collectMergedDetailIndicators(
                discoveryService.discover(page),
                groupCode,
                namespace,
                serviceRootName,
                outcomesType,
                requestedPeriod,
                useFirstRootAsAggregateParent
        );
    }

    private List<ExternalIndicatorRow> collectMergedDetailIndicators(
            IminfinReportDefinition report,
            IndicatorGroupCode groupCode,
            String namespace,
            String serviceRootName,
            Integer outcomesType,
            String requestedPeriod,
            boolean useFirstRootAsAggregateParent
    ) {
        String dataSourceCode = reportDataLoader.resolveDetailDataSourceCode(report, requestedPeriod);
        List<ExternalRegionRow> regions = collectRegions();
        List<List<IminfinParsedIndicatorRow>> regionTrees = mapInParallel(regions, region -> {
            IminfinLoadedData loaded = reportDataLoader.loadDetailData(
                    report,
                    dataSourceCode,
                    region.externalCode(),
                    requestedPeriod,
                    outcomesType
            );
            return prepareDetailTreeRows(
                    namespace,
                    serviceRootName,
                    useFirstRootAsAggregateParent,
                    indicatorTreeParser.parseDetailRows(namespace, loaded.dataSource(), loaded.dataRows())
            );
        });

        return mergeIndicatorTrees(groupCode, namespace, serviceRootName, regionTrees);
    }

    private List<ExternalDatasetPayload> collectDetailObservationPayloads(
            IminfinPassportPage page,
            IndicatorGroupCode groupCode,
            String namespace,
            String serviceRootName,
            Integer outcomesType,
            String requestedPeriod,
            Collection<ExternalRegionRef> regions,
            boolean useFirstRootAsAggregateParent
    ) {
        return collectDetailObservationPayloads(
                discoveryService.discover(page),
                groupCode,
                namespace,
                serviceRootName,
                outcomesType,
                requestedPeriod,
                regions,
                useFirstRootAsAggregateParent
        );
    }

    private List<ExternalDatasetPayload> collectDetailObservationPayloads(
            IminfinReportDefinition report,
            IndicatorGroupCode groupCode,
            String namespace,
            String serviceRootName,
            Integer outcomesType,
            String requestedPeriod,
            Collection<ExternalRegionRef> regions,
            boolean useFirstRootAsAggregateParent
    ) {
        String dataSourceCode = reportDataLoader.resolveDetailDataSourceCode(report, requestedPeriod);
        return mapInParallel(regions, region -> {
            IminfinLoadedData loaded = reportDataLoader.loadDetailData(report, dataSourceCode, region.externalCode(), requestedPeriod, outcomesType);
            List<IminfinParsedIndicatorRow> parsedRows = prepareDetailTreeRows(
                    namespace,
                    serviceRootName,
                    useFirstRootAsAggregateParent,
                    indicatorTreeParser.parseDetailRows(namespace, loaded.dataSource(), loaded.dataRows())
            );
            List<ExternalObservationRow> observations = observationMapper.mapDetailObservations(
                    region.externalCode(),
                    groupCode,
                    loaded.dataSource(),
                    parsedRows
            );
            return toPayload(report, loaded, observations);
        });
    }

    private IminfinParsedIndicatorRow applyServiceRoot(
            IminfinParsedIndicatorRow row,
            String namespace,
            String serviceRootName
    ) {
        if (serviceRootName == null || serviceRootName.isBlank()) {
            return row;
        }
        return new IminfinParsedIndicatorRow(
                row.naturalKey(),
                row.parentNaturalKey() == null ? namespace : row.parentNaturalKey(),
                row.name(),
                row.parentName() == null ? serviceRootName : row.parentName(),
                row.level() + 1,
                row.sortOrder() + 1,
                row.hasChildren(),
                row.row()
        );
    }

    private List<IminfinParsedIndicatorRow> prepareDetailTreeRows(
            String namespace,
            String serviceRootName,
            boolean useFirstRootAsAggregateParent,
            List<IminfinParsedIndicatorRow> rows
    ) {
        List<IminfinParsedIndicatorRow> prepared = useFirstRootAsAggregateParent
                ? reparentTopLevelRowsToFirstRoot(rows)
                : rows;
        return prepared.stream()
                .map(row -> applyServiceRoot(row, namespace, serviceRootName))
                .toList();
    }

    private List<IminfinParsedIndicatorRow> reparentTopLevelRowsToFirstRoot(List<IminfinParsedIndicatorRow> rows) {
        if (rows == null || rows.size() < 2) {
            return rows == null ? List.of() : rows;
        }

        IminfinParsedIndicatorRow aggregateRoot = rows.stream()
                .filter(this::isSourceTopLevelRoot)
                .findFirst()
                .orElse(null);
        if (aggregateRoot == null) {
            return rows;
        }

        List<IminfinParsedIndicatorRow> adjusted = new ArrayList<>(rows.size());
        boolean shiftCurrentSiblingSubtree = false;
        boolean changed = false;

        for (IminfinParsedIndicatorRow row : rows) {
            if (row == null) {
                continue;
            }

            if (row == aggregateRoot) {
                shiftCurrentSiblingSubtree = false;
                adjusted.add(row);
                continue;
            }

            if (isSourceTopLevelRoot(row)) {
                shiftCurrentSiblingSubtree = true;
                changed = true;
                adjusted.add(new IminfinParsedIndicatorRow(
                        row.naturalKey(),
                        aggregateRoot.naturalKey(),
                        row.name(),
                        aggregateRoot.name(),
                        row.level() + 1,
                        row.sortOrder(),
                        row.hasChildren(),
                        row.row()
                ));
                continue;
            }

            if (shiftCurrentSiblingSubtree) {
                changed = true;
                adjusted.add(new IminfinParsedIndicatorRow(
                        row.naturalKey(),
                        row.parentNaturalKey(),
                        row.name(),
                        row.parentName(),
                        row.level() + 1,
                        row.sortOrder(),
                        row.hasChildren(),
                        row.row()
                ));
            } else {
                adjusted.add(row);
            }
        }

        return changed ? refreshHasChildren(adjusted) : rows;
    }

    private boolean isSourceTopLevelRoot(IminfinParsedIndicatorRow row) {
        return row != null
                && row.level() == 0
                && (row.parentNaturalKey() == null || row.parentNaturalKey().isBlank());
    }

    private List<IminfinParsedIndicatorRow> refreshHasChildren(List<IminfinParsedIndicatorRow> rows) {
        Set<String> parentKeys = rows.stream()
                .map(IminfinParsedIndicatorRow::parentNaturalKey)
                .filter(Objects::nonNull)
                .filter(parentKey -> !parentKey.isBlank())
                .collect(HashSet::new, Set::add, Set::addAll);
        return rows.stream()
                .map(row -> new IminfinParsedIndicatorRow(
                        row.naturalKey(),
                        row.parentNaturalKey(),
                        row.name(),
                        row.parentName(),
                        row.level(),
                        row.sortOrder(),
                        parentKeys.contains(row.naturalKey()),
                        row.row()
                ))
                .toList();
    }

    private List<ExternalIndicatorRow> mergeIndicatorTrees(
            IndicatorGroupCode groupCode,
            String namespace,
            String serviceRootName,
            List<List<IminfinParsedIndicatorRow>> regionTrees
    ) {
        Map<IndicatorRowIdentity, MergedIndicatorRow> mergedRows = new LinkedHashMap<>();
        Map<IndicatorRowIdentity, String> naturalKeyByIdentity = new LinkedHashMap<>();
        Set<String> usedNaturalKeys = new HashSet<>();
        int sortOrder = 0;

        if (serviceRootName != null && !serviceRootName.isBlank()) {
            IndicatorRowIdentity rootIdentity = IndicatorRowIdentity.from(serviceRootName, null);
            String rootKey = uniqueNaturalKey(namespace, usedNaturalKeys);
            naturalKeyByIdentity.put(rootIdentity, rootKey);
            mergedRows.put(rootIdentity, new MergedIndicatorRow(
                    groupCode,
                    rootKey,
                    serviceRootName,
                    null,
                    0,
                    ++sortOrder,
                    true
            ));
        }

        for (List<IminfinParsedIndicatorRow> tree : regionTrees) {
            if (tree == null || tree.isEmpty()) {
                continue;
            }

            Map<String, IndicatorRowIdentity> identityBySourceNaturalKey = new HashMap<>();
            for (IminfinParsedIndicatorRow row : tree) {
                if (row != null && row.naturalKey() != null && row.name() != null && !row.name().isBlank()) {
                    identityBySourceNaturalKey.put(row.naturalKey(), IndicatorRowIdentity.from(row.name(), row.parentName()));
                }
            }

            for (IminfinParsedIndicatorRow row : tree) {
                if (row == null || row.name() == null || row.name().isBlank()) {
                    continue;
                }
                IndicatorRowIdentity identity = IndicatorRowIdentity.from(row.name(), row.parentName());
                MergedIndicatorRow existing = mergedRows.get(identity);
                if (existing != null) {
                    if (row.hasChildren() && !existing.hasChildren()) {
                        mergedRows.put(identity, existing.withChildren());
                    }
                    continue;
                }

                String parentNaturalKey = null;
                if (row.parentNaturalKey() != null && !row.parentNaturalKey().isBlank()) {
                    IndicatorRowIdentity parentIdentity;
                    if (serviceRootName != null && row.parentNaturalKey().equals(namespace)) {
                        parentIdentity = IndicatorRowIdentity.from(serviceRootName, null);
                    } else {
                        parentIdentity = identityBySourceNaturalKey.get(row.parentNaturalKey());
                    }
                    parentNaturalKey = parentIdentity == null ? null : naturalKeyByIdentity.get(parentIdentity);
                }

                String naturalKey = uniqueNaturalKey(buildNaturalKey(namespace, row.name(), row.parentName()), usedNaturalKeys);
                naturalKeyByIdentity.put(identity, naturalKey);
                mergedRows.put(identity, new MergedIndicatorRow(
                        groupCode,
                        naturalKey,
                        row.name(),
                        parentNaturalKey,
                        row.level(),
                        ++sortOrder,
                        row.hasChildren()
                ));
            }
        }

        return mergedRows.values().stream()
                .map(MergedIndicatorRow::toExternalRow)
                .toList();
    }

    private String buildNaturalKey(String namespace, String name, String parentName) {
        String nameSegment = TextNormalizer.slugify(name);
        if (nameSegment.isBlank()) {
            nameSegment = "indicator";
        }
        if (parentName == null || parentName.isBlank()) {
            return namespace + "/" + nameSegment;
        }
        String parentSegment = TextNormalizer.slugify(parentName);
        if (parentSegment.isBlank()) {
            parentSegment = "parent";
        }
        return namespace + "/" + parentSegment + "/" + nameSegment;
    }

    private String uniqueNaturalKey(String baseKey, Set<String> usedNaturalKeys) {
        String key = baseKey;
        int duplicate = 2;
        while (!usedNaturalKeys.add(key)) {
            key = baseKey + "-" + duplicate;
            duplicate++;
        }
        return key;
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
        return mapInParallel(creditIndicatorSpecs(source), spec -> {
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
                    spec.parentName(),
                    loaded.dataSource(),
                    loaded.dataRows(),
                    externalCodeByName
            );
            return toPayload(report, loaded, observations);
        });
    }

    private <T, R> List<R> mapInParallel(Collection<T> items, Function<T, R> mapper) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<T> orderedItems = List.copyOf(items);
        int parallelism = Math.min(properties.maxParallelRequests(), orderedItems.size());
        if (parallelism <= 1) {
            return orderedItems.stream().map(mapper).toList();
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            List<CompletableFuture<R>> futures = orderedItems.stream()
                    .map(item -> CompletableFuture.supplyAsync(() -> mapper.apply(item), executor))
                    .toList();
            List<R> result = new ArrayList<>(futures.size());
            for (CompletableFuture<R> future : futures) {
                result.add(future.join());
            }
            return result;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Parallel iMinfin collection failed: " + cause.getMessage(), cause);
        } finally {
            executor.shutdownNow();
        }
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
        String currentParentName = null;
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
            String parentName = child ? currentParentName : null;
            String baseKey = parentKey == null
                    ? "credit/" + TextNormalizer.slugify(cleanCaption)
                    : parentKey + "/" + TextNormalizer.slugify(cleanCaption);
            String key = uniqueNaturalKey(baseKey, usedKeys);
            if (!child) {
                currentParentKey = key;
                currentParentName = cleanCaption;
            }
            result.add(new CreditIndicatorSpec(key, parentKey, cleanCaption, parentName, child ? 1 : 0, sortOrder, parameterValue));
        }
        return result;
    }

    private record IndicatorRowIdentity(String normalizedName, String normalizedParentName) {
        private static IndicatorRowIdentity from(String name, String parentName) {
            return new IndicatorRowIdentity(TextNormalizer.normalize(name), normalizeNullable(parentName));
        }

        private static String normalizeNullable(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = TextNormalizer.normalize(value);
            return normalized.isBlank() ? null : normalized;
        }
    }

    private record MergedIndicatorRow(
            IndicatorGroupCode groupCode,
            String naturalKey,
            String name,
            String parentNaturalKey,
            int level,
            int sortOrder,
            boolean hasChildren
    ) {
        private MergedIndicatorRow withChildren() {
            return new MergedIndicatorRow(groupCode, naturalKey, name, parentNaturalKey, level, sortOrder, true);
        }

        private ExternalIndicatorRow toExternalRow() {
            return new ExternalIndicatorRow(groupCode, naturalKey, name, parentNaturalKey, level, sortOrder, hasChildren);
        }
    }

    private record OutcomeTypeSpec(int value, String namespace, String serviceRootName) {
    }

    private record CreditIndicatorSpec(
            String naturalKey,
            String parentNaturalKey,
            String name,
            String parentName,
            int level,
            int sortOrder,
            String sourceParameterValue
    ) {
    }
}
