package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.CollectObservationsCommand;
import Ogni.ODAS.application.command.analysis.*;
import Ogni.ODAS.application.dto.analysis.*;
import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.in.AnalysisUseCase;
import Ogni.ODAS.application.port.in.ObservationCollectionUseCase;
import Ogni.ODAS.application.port.out.analysis.*;
import Ogni.ODAS.application.port.out.persistence.AnalysisQueryPort;
import Ogni.ODAS.application.port.out.persistence.PeriodPersistencePort;
import Ogni.ODAS.application.port.out.persistence.StoredDataQueryPort;
import Ogni.ODAS.domain.enumtype.ObservationValueType;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.Period;

import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnalysisService implements AnalysisUseCase {

    private final ObservationCollectionUseCase observationCollectionUseCase;
    private final PeriodPersistencePort periodPersistence;
    private final StoredDataQueryPort storedDataQuery;
    private final AnalysisQueryPort analysisQuery;
    private final NonCumulativeValuePort nonCumulativeValuePort;
    private final QuarterAggregationPort quarterAggregationPort;
    private final PeriodGrowthMetricsPort periodGrowthMetricsPort;
    private final RegionComparisonPort regionComparisonPort;
    private final SubtreeSlicePort subtreeSlicePort;
    private final RegionIndicatorMatrixPort regionIndicatorMatrixPort;

    public AnalysisService(
            ObservationCollectionUseCase observationCollectionUseCase,
            PeriodPersistencePort periodPersistence,
            StoredDataQueryPort storedDataQuery,
            AnalysisQueryPort analysisQuery,
            NonCumulativeValuePort nonCumulativeValuePort,
            QuarterAggregationPort quarterAggregationPort,
            PeriodGrowthMetricsPort periodGrowthMetricsPort,
            RegionComparisonPort regionComparisonPort,
            SubtreeSlicePort subtreeSlicePort,
            RegionIndicatorMatrixPort regionIndicatorMatrixPort
    ) {
        this.observationCollectionUseCase = Objects.requireNonNull(observationCollectionUseCase);
        this.periodPersistence = Objects.requireNonNull(periodPersistence);
        this.storedDataQuery = Objects.requireNonNull(storedDataQuery);
        this.analysisQuery = Objects.requireNonNull(analysisQuery);
        this.nonCumulativeValuePort = Objects.requireNonNull(nonCumulativeValuePort);
        this.quarterAggregationPort = Objects.requireNonNull(quarterAggregationPort);
        this.periodGrowthMetricsPort = Objects.requireNonNull(periodGrowthMetricsPort);
        this.regionComparisonPort = Objects.requireNonNull(regionComparisonPort);
        this.subtreeSlicePort = Objects.requireNonNull(subtreeSlicePort);
        this.regionIndicatorMatrixPort = Objects.requireNonNull(regionIndicatorMatrixPort);
    }

    @Override
    public RegionComparisonResultDto compareRegions(CompareRegionsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, command.year(), null, null);

        List<RegionReadDto> regions = selectedRegions(command.regionIds());
        String indicatorName = resolveIndicatorName(command.groupCode(), yearPeriod, command.indicatorYearEntryId());
        List<ObservationReadDto> observations = findMonthObservationsWithAutoCollect(
                command.groupCode(),
                command.year(),
                command.month(),
                command.regionIds(),
                List.of(command.indicatorYearEntryId()),
                Set.of(command.valueKind()),
                command.forceRefresh()
        );
        return regionComparisonPort.calculate(
                command.groupCode(),
                command.year(),
                command.month(),
                command.indicatorYearEntryId(),
                indicatorName,
                command.valueKind(),
                command.valueKind().getUnitCode(),
                regions,
                observations
        );
    }

    @Override
    public MonthlySeriesResultDto buildMonthlySeries(BuildMonthlySeriesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        RegionReadDto region = requireRegion(command.regionId());
        IndicatorContext indicatorContext = requireIndicatorContext(command.groupCode(), command.year(), command.indicatorYearEntryId());

        CoveragePlan plan = buildSeriesCoverage(command.year(), command.month());
        MonthlyDataResolution resolution = resolveMonthlyData(
                command.groupCode(),
                command.regionId(),
                indicatorContext,
                command.valueKind(),
                plan,
                command.autoCollectMissing(),
                command.forceRefresh(),
                NonCumulativeValueMode.SERIES_RANGE,
                command.year(),
                command.month()
        );

        List<MonthlySeriesPointDto> visiblePoints = filterVisiblePoints(resolution.points(), plan.visibleMonths());
        List<QuarterAggregateDto> quarterAggregates = command.includeQuarterAggregates()
                ? quarterAggregationPort.aggregate(visiblePoints)
                : List.of();

        return new MonthlySeriesResultDto(
                command.groupCode(),
                region.id(),
                region.name(),
                command.indicatorYearEntryId(),
                indicatorContext.targetEntry().name(),
                command.valueKind(),
                command.valueKind().getLabel(),
                command.valueKind().getUnitCode(),
                command.valueKind().getUnitCode().getLabel(),
                NonCumulativeValueMode.SERIES_RANGE,
                command.year(),
                command.month(),
                plan.visibleMonths().size(),
                visiblePoints.size(),
                resolution.autoCollectedMissing(),
                visiblePoints,
                quarterAggregates
        );
    }

    @Override
    public PeriodGrowthMetricsResultDto calculatePeriodGrowthMetrics(CalculatePeriodGrowthMetricsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        RegionReadDto region = requireRegion(command.regionId());
        IndicatorContext indicatorContext = requireIndicatorContext(command.groupCode(), command.year(), command.indicatorYearEntryId());

        CoveragePlan plan = buildMetricsCoverage(command.year(), command.month());
        MonthlyDataResolution resolution = resolveMonthlyData(
                command.groupCode(),
                command.regionId(),
                indicatorContext,
                command.valueKind(),
                plan,
                command.autoCollectMissing(),
                command.forceRefresh(),
                NonCumulativeValueMode.TARGET_MONTH_AND_QUARTER_METRICS,
                command.year(),
                command.month()
        );
        List<MonthlySeriesPointDto> visiblePoints = filterVisiblePoints(resolution.points(), plan.visibleMonths());
        List<QuarterAggregateDto> quarterAggregates = quarterAggregationPort.aggregate(visiblePoints);
        PeriodGrowthMetricsPort.MetricsSnapshot snapshot = periodGrowthMetricsPort.calculate(
                visiblePoints,
                quarterAggregates,
                command.year(),
                command.month()
        );
        return snapshot.applyMetadata(
                command.groupCode(),
                region.id(),
                region.name(),
                command.indicatorYearEntryId(),
                indicatorContext.targetEntry().name(),
                command.valueKind(),
                command.valueKind().getLabel(),
                command.valueKind().getUnitCode(),
                command.valueKind().getUnitCode().getLabel(),
                command.year(),
                command.month(),
                resolution.autoCollectedMissing()
        );
    }

    @Override
    public SubtreeSliceResultDto buildSubtreeSlice(BuildSubtreeSliceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        RegionReadDto region = requireRegion(command.regionId());
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, command.year(), null, null);

        List<IndicatorEntryReadDto> allEntries = yearPeriod
                .map(period -> storedDataQuery.findIndicatorEntries(command.groupCode(), period.id()))
                .orElseGet(List::of);
        Map<Long, IndicatorEntryReadDto> entriesById = allEntries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        IndicatorEntryReadDto rootEntry = entriesById.get(command.rootIndicatorYearEntryId());
        if (rootEntry == null) {
            throw new IllegalArgumentException("Indicator entry " + command.rootIndicatorYearEntryId() + " was not found for year " + command.year());
        }
        List<IndicatorEntryReadDto> subtreeEntries = collectSubtreeEntries(rootEntry.id(), allEntries);
        List<Long> entryIds = subtreeEntries.stream().map(IndicatorEntryReadDto::id).toList();
        List<ObservationReadDto> observations = findMonthObservationsWithAutoCollect(
                command.groupCode(),
                command.year(),
                command.month(),
                List.of(command.regionId()),
                entryIds,
                Set.of(command.valueKind()),
                command.forceRefresh()
        );

        return subtreeSlicePort.calculate(
                command.groupCode(),
                command.year(),
                command.month(),
                region,
                rootEntry,
                command.valueKind(),
                command.valueKind().getUnitCode(),
                subtreeEntries,
                observations
        );
    }

    @Override
    public RegionIndicatorMatrixResultDto buildRegionIndicatorMatrix(BuildRegionIndicatorMatrixCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, command.year(), null, null);
        List<RegionReadDto> rows = selectedRegions(command.regionIds());
        List<IndicatorEntryReadDto> columns = yearPeriod
                .map(period -> storedDataQuery.findIndicatorEntries(command.groupCode(), period.id()))
                .orElseGet(List::of)
                .stream()
                .filter(entry -> command.indicatorYearEntryIds().contains(entry.id()))
                .sorted(Comparator.comparing(IndicatorEntryReadDto::level)
                        .thenComparing(IndicatorEntryReadDto::sortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(IndicatorEntryReadDto::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<ObservationReadDto> observations = findMonthObservationsWithAutoCollect(
                command.groupCode(),
                command.year(),
                command.month(),
                command.regionIds(),
                command.indicatorYearEntryIds(),
                Set.of(command.valueKind()),
                command.forceRefresh()
        );

        return regionIndicatorMatrixPort.calculate(
                command.groupCode(),
                command.year(),
                command.month(),
                command.valueKind(),
                command.valueKind().getUnitCode(),
                rows,
                columns,
                observations
        );
    }

    private MonthlyDataResolution resolveMonthlyData(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            Long regionId,
            IndicatorContext indicatorContext,
            Ogni.ODAS.domain.enumtype.ObservationValueKind valueKind,
            CoveragePlan coveragePlan,
            boolean autoCollectMissing,
            boolean forceRefresh,
            NonCumulativeValueMode mode,
            int targetYear,
            int targetMonth
    ) {
        MonthlyDataResolution initial = resolveMonthlyDataInternal(
                groupCode,
                regionId,
                indicatorContext,
                valueKind,
                coveragePlan,
                autoCollectMissing,
                forceRefresh,
                mode
        );
        if (autoCollectMissing || !requiresAutoCollectionRetry(initial.loadedMonths(), coveragePlan, mode, targetYear, targetMonth)) {
            return initial;
        }
        MonthlyDataResolution forced = resolveMonthlyDataInternal(
                groupCode,
                regionId,
                indicatorContext,
                valueKind,
                coveragePlan,
                true,
                forceRefresh,
                mode
        );
        return forced.loadedMonths().equals(initial.loadedMonths()) && !forced.autoCollectedMissing()
                ? initial
                : forced;
    }

    private MonthlyDataResolution resolveMonthlyDataInternal(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            Long regionId,
            IndicatorContext indicatorContext,
            Ogni.ODAS.domain.enumtype.ObservationValueKind valueKind,
            CoveragePlan coveragePlan,
            boolean autoCollectMissing,
            boolean forceRefresh,
            NonCumulativeValueMode mode
    ) {
        if (forceRefresh) {
            for (YearMonth month : coveragePlan.fetchMonths()) {
                observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, month.getYear(), month.getMonthValue(), List.of(regionId)));
            }
        }

        IndicatorResolutionResult indicatorResolution = resolveIndicatorEntriesByYear(groupCode, regionId, indicatorContext, coveragePlan.fetchMonths(), autoCollectMissing);
        Map<Integer, Long> indicatorEntryIdsByYear = indicatorResolution.entryIdsByYear();

        RawPointLoadResult firstPass = loadRawPoints(groupCode, regionId, valueKind, coveragePlan.fetchMonths(), indicatorEntryIdsByYear);
        LinkedHashSet<YearMonth> missingMonths = new LinkedHashSet<>(coveragePlan.fetchMonths());
        missingMonths.removeAll(firstPass.loadedMonths());
        boolean autoCollected = false;
        if (autoCollectMissing && !missingMonths.isEmpty()) {
            for (YearMonth month : missingMonths) {
                observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, month.getYear(), month.getMonthValue(), List.of(regionId)));
            }
            autoCollected = true;
            indicatorEntryIdsByYear = resolveIndicatorEntriesByYear(groupCode, regionId, indicatorContext, coveragePlan.fetchMonths(), false).entryIdsByYear();
        }
        RawPointLoadResult resolved = autoCollected
                ? loadRawPoints(groupCode, regionId, valueKind, coveragePlan.fetchMonths(), indicatorEntryIdsByYear)
                : firstPass;

        List<MonthlySeriesPointDto> points = nonCumulativeValuePort.calculate(
                resolved.points(),
                valueKind.getObservationValueType() == ObservationValueType.ABSOLUTE,
                mode
        );
        return new MonthlyDataResolution(points, resolved.loadedMonths(), autoCollected || indicatorResolution.autoCollectedMissing());
    }

    private List<ObservationReadDto> findMonthObservationsWithAutoCollect(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            int year,
            int month,
            Collection<Long> regionIds,
            Collection<Long> indicatorYearEntryIds,
            Set<Ogni.ODAS.domain.enumtype.ObservationValueKind> valueKinds,
            boolean forceRefresh
    ) {
        if (forceRefresh) {
            observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, year, month, List.copyOf(regionIds)));
            return findMonthObservations(groupCode, year, month, regionIds, indicatorYearEntryIds, valueKinds);
        }

        List<ObservationReadDto> observations = findMonthObservations(groupCode, year, month, regionIds, indicatorYearEntryIds, valueKinds);
        if (!observations.isEmpty()) {
            return observations;
        }
        observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, year, month, List.copyOf(regionIds)));
        return findMonthObservations(groupCode, year, month, regionIds, indicatorYearEntryIds, valueKinds);
    }

    private List<ObservationReadDto> findMonthObservations(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            int year,
            int month,
            Collection<Long> regionIds,
            Collection<Long> indicatorYearEntryIds,
            Set<Ogni.ODAS.domain.enumtype.ObservationValueKind> valueKinds
    ) {
        return periodPersistence.findByIdentity(PeriodType.MONTH, year, month, null)
                .map(period -> storedDataQuery.findObservations(
                        groupCode,
                        period.id(),
                        regionIds,
                        indicatorYearEntryIds,
                        valueKinds
                ))
                .orElseGet(List::of);
    }

    private boolean requiresAutoCollectionRetry(
            Set<YearMonth> loadedMonths,
            CoveragePlan coveragePlan,
            NonCumulativeValueMode mode,
            int targetYear,
            int targetMonth
    ) {
        YearMonth target = YearMonth.of(targetYear, targetMonth);
        if (!loadedMonths.contains(target)) {
            return true;
        }
        if (mode == NonCumulativeValueMode.TARGET_MONTH_AND_QUARTER_METRICS) {
            return !loadedMonths.containsAll(coveragePlan.visibleMonths());
        }
        return false;
    }

    private RawPointLoadResult loadRawPoints(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            Long regionId,
            Ogni.ODAS.domain.enumtype.ObservationValueKind valueKind,
            Set<YearMonth> fetchMonths,
            Map<Integer, Long> indicatorEntryIdsByYear
    ) {
        List<MonthlyObservationPointRawDto> result = new ArrayList<>();
        Set<YearMonth> loadedMonths = new LinkedHashSet<>();
        Map<Integer, List<YearMonth>> monthsByYear = fetchMonths.stream()
                .collect(Collectors.groupingBy(YearMonth::getYear, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<YearMonth>> entry : monthsByYear.entrySet()) {
            Long indicatorYearEntryId = indicatorEntryIdsByYear.get(entry.getKey());
            if (indicatorYearEntryId == null) {
                continue;
            }
            List<Long> periodIds = entry.getValue().stream()
                    .map(period -> periodPersistence.findByIdentity(PeriodType.MONTH, period.getYear(), period.getMonthValue(), null))
                    .flatMap(Optional::stream)
                    .map(Period::id)
                    .toList();
            if (periodIds.isEmpty()) {
                continue;
            }
            List<MonthlyObservationPointRawDto> points = analysisQuery.findMonthlyObservationPoints(
                    groupCode,
                    regionId,
                    indicatorYearEntryId,
                    valueKind,
                    periodIds
            );
            result.addAll(points);
            for (MonthlyObservationPointRawDto point : points) {
                loadedMonths.add(YearMonth.of(point.year(), point.month()));
            }
        }
        result.sort(Comparator.comparing(MonthlyObservationPointRawDto::year).thenComparing(MonthlyObservationPointRawDto::month));
        return new RawPointLoadResult(List.copyOf(result), Set.copyOf(loadedMonths));
    }

    private IndicatorResolutionResult resolveIndicatorEntriesByYear(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            Long regionId,
            IndicatorContext indicatorContext,
            Set<YearMonth> months,
            boolean autoCollectMissing
    ) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        boolean autoCollected = false;
        Set<Integer> years = months.stream().map(YearMonth::getYear).collect(Collectors.toCollection(LinkedHashSet::new));
        result.put(indicatorContext.targetYear(), indicatorContext.targetEntry().id());
        for (Integer year : years) {
            if (Objects.equals(year, indicatorContext.targetYear())) {
                continue;
            }
            Optional<Long> mapped = resolveIndicatorEntryForYear(groupCode, indicatorContext, year);
            if (mapped.isEmpty() && autoCollectMissing) {
                int sampleMonth = months.stream().filter(month -> month.getYear() == year).findFirst().map(YearMonth::getMonthValue).orElse(1);
                observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, year, sampleMonth, List.of(regionId)));
                autoCollected = true;
                mapped = resolveIndicatorEntryForYear(groupCode, indicatorContext, year);
            }
            mapped.ifPresent(value -> result.put(year, value));
        }
        return new IndicatorResolutionResult(result, autoCollected);
    }

    private Optional<Long> resolveIndicatorEntryForYear(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            IndicatorContext indicatorContext,
            int year
    ) {
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, year, null, null);
        if (yearPeriod.isEmpty()) {
            return Optional.empty();
        }
        List<IndicatorEntryReadDto> entries = storedDataQuery.findIndicatorEntries(groupCode, yearPeriod.get().id());
        Map<Long, String> pathByEntryId = buildPathSignatures(entries);
        return entries.stream()
                .map(IndicatorEntryReadDto::id)
                .filter(id -> Objects.equals(pathByEntryId.get(id), indicatorContext.pathSignature()))
                .findFirst();
    }

    private IndicatorContext requireIndicatorContext(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            int year,
            Long indicatorYearEntryId
    ) {
        Optional<Period> yearPeriod = periodPersistence.findByIdentity(PeriodType.YEAR, year, null, null);
        if (yearPeriod.isEmpty()) {
            throw new IllegalArgumentException("Year period " + year + " was not found");
        }
        List<IndicatorEntryReadDto> entries = storedDataQuery.findIndicatorEntries(groupCode, yearPeriod.get().id());
        Map<Long, IndicatorEntryReadDto> byId = entries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        IndicatorEntryReadDto targetEntry = byId.get(indicatorYearEntryId);
        if (targetEntry == null) {
            throw new IllegalArgumentException("Indicator entry " + indicatorYearEntryId + " was not found for year " + year);
        }
        Map<Long, String> pathByEntryId = buildPathSignatures(entries);
        return new IndicatorContext(year, targetEntry, pathByEntryId.get(targetEntry.id()));
    }

    private String resolveIndicatorName(
            Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
            Optional<Period> yearPeriod,
            Long indicatorYearEntryId
    ) {
        return yearPeriod
                .map(period -> storedDataQuery.findIndicatorEntries(groupCode, period.id()))
                .orElseGet(List::of)
                .stream()
                .filter(entry -> Objects.equals(entry.id(), indicatorYearEntryId))
                .findFirst()
                .map(IndicatorEntryReadDto::name)
                .orElse(null);
    }

    private List<IndicatorEntryReadDto> collectSubtreeEntries(Long rootId, List<IndicatorEntryReadDto> allEntries) {
        Map<Long, List<IndicatorEntryReadDto>> childrenByParent = allEntries.stream()
                .filter(entry -> entry.parentIndicatorYearEntryId() != null)
                .collect(Collectors.groupingBy(
                        IndicatorEntryReadDto::parentIndicatorYearEntryId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<IndicatorEntryReadDto> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>(List.of(rootId));
        Map<Long, IndicatorEntryReadDto> byId = allEntries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            IndicatorEntryReadDto entry = byId.get(current);
            if (entry == null) {
                continue;
            }
            result.add(entry);
            List<IndicatorEntryReadDto> children = childrenByParent.getOrDefault(current, List.of()).stream()
                    .sorted(Comparator.comparing(IndicatorEntryReadDto::level)
                            .thenComparing(IndicatorEntryReadDto::sortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(IndicatorEntryReadDto::name, Comparator.nullsLast(String::compareTo)))
                    .toList();
            for (IndicatorEntryReadDto child : children) {
                queue.addLast(child.id());
            }
        }
        return List.copyOf(result);
    }

    private Map<Long, String> buildPathSignatures(List<IndicatorEntryReadDto> entries) {
        Map<Long, IndicatorEntryReadDto> byId = entries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> cache = new LinkedHashMap<>();
        for (IndicatorEntryReadDto entry : entries) {
            buildPathSignature(entry, byId, cache);
        }
        return cache;
    }

    private String buildPathSignature(
            IndicatorEntryReadDto entry,
            Map<Long, IndicatorEntryReadDto> byId,
            Map<Long, String> cache
    ) {
        String cached = cache.get(entry.id());
        if (cached != null) {
            return cached;
        }
        String current = Long.toString(entry.indicatorId());
        if (entry.parentIndicatorYearEntryId() == null) {
            cache.put(entry.id(), current);
            return current;
        }
        IndicatorEntryReadDto parent = byId.get(entry.parentIndicatorYearEntryId());
        String signature = parent == null ? current : buildPathSignature(parent, byId, cache) + "/" + current;
        cache.put(entry.id(), signature);
        return signature;
    }

    private List<MonthlySeriesPointDto> filterVisiblePoints(List<MonthlySeriesPointDto> points, Set<YearMonth> visibleMonths) {
        return points.stream()
                .filter(point -> visibleMonths.contains(YearMonth.of(point.year(), point.month())))
                .sorted(Comparator.comparing(MonthlySeriesPointDto::year).thenComparing(MonthlySeriesPointDto::month))
                .toList();
    }

    private RegionReadDto requireRegion(Long regionId) {
        return selectedRegions(List.of(regionId)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Region " + regionId + " was not found"));
    }

    private List<RegionReadDto> selectedRegions(List<Long> regionIds) {
        Set<Long> requested = new LinkedHashSet<>(regionIds);
        return storedDataQuery.findRegions().stream()
                .filter(region -> requested.contains(region.id()))
                .toList();
    }

    private CoveragePlan buildSeriesCoverage(int year, int month) {
        LinkedHashSet<YearMonth> visible = new LinkedHashSet<>();
        for (int currentMonth = 1; currentMonth <= month; currentMonth++) {
            visible.add(YearMonth.of(year, currentMonth));
        }
        return new CoveragePlan(visible, visible);
    }

    private CoveragePlan buildMetricsCoverage(int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        LinkedHashSet<YearMonth> visible = new LinkedHashSet<>();
        visible.add(target.minusMonths(12));
        visible.add(target.minusMonths(1));
        visible.add(target);

        int currentQuarter = quarter(month);
        YearMonth currentQuarterStart = YearMonth.of(year, ((currentQuarter - 1) * 3) + 1);
        addQuarterMonths(visible, currentQuarterStart);

        YearMonth previousQuarterStart = currentQuarterStart.minusMonths(3);
        addQuarterMonths(visible, previousQuarterStart);

        YearMonth sameQuarterPreviousYearStart = currentQuarterStart.minusYears(1);
        addQuarterMonths(visible, sameQuarterPreviousYearStart);

        LinkedHashSet<YearMonth> fetch = new LinkedHashSet<>(visible);
        for (YearMonth visibleMonth : visible) {
            if (visibleMonth.getMonthValue() == 1) {
                continue;
            }
            fetch.add(visibleMonth.minusMonths(1));
        }
        return new CoveragePlan(fetch, visible);
    }

    private void addQuarterMonths(Set<YearMonth> target, YearMonth quarterStart) {
        target.add(quarterStart);
        target.add(quarterStart.plusMonths(1));
        target.add(quarterStart.plusMonths(2));
    }

    private int quarter(int month) {
        return ((month - 1) / 3) + 1;
    }

    private record CoveragePlan(Set<YearMonth> fetchMonths, Set<YearMonth> visibleMonths) {
    }

    private record IndicatorContext(int targetYear, IndicatorEntryReadDto targetEntry, String pathSignature) {
    }

    private record RawPointLoadResult(List<MonthlyObservationPointRawDto> points, Set<YearMonth> loadedMonths) {
    }

    private record IndicatorResolutionResult(Map<Integer, Long> entryIdsByYear, boolean autoCollectedMissing) {
    }

    private record MonthlyDataResolution(List<MonthlySeriesPointDto> points, Set<YearMonth> loadedMonths,
                                         boolean autoCollectedMissing) {
    }
}
