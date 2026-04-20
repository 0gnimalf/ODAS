package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ExternalIndicatorRow;
import Ogni.ODAS.application.dto.ExternalRegionRow;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceSyncUseCase;
import Ogni.ODAS.application.port.out.collector.ExternalIndicatorCollectorPort;
import Ogni.ODAS.application.port.out.collector.ExternalRegionCollectorPort;
import Ogni.ODAS.application.port.out.persistence.IndicatorPersistencePort;
import Ogni.ODAS.application.port.out.persistence.IndicatorYearEntryPersistencePort;
import Ogni.ODAS.application.port.out.persistence.PeriodPersistencePort;
import Ogni.ODAS.application.port.out.persistence.RegionPersistencePort;
import Ogni.ODAS.application.support.SourceRegionCode;
import Ogni.ODAS.application.support.StaticRegionCatalog;
import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.IndicatorYearEntry;
import Ogni.ODAS.domain.model.Period;
import Ogni.ODAS.domain.model.Region;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReferenceSyncService implements ReferenceSyncUseCase {

    private static final List<IndicatorGroupCode> DEFAULT_GROUPS = List.of(
            IndicatorGroupCode.INCOME,
            IndicatorGroupCode.OUTCOME,
            IndicatorGroupCode.CREDIT,
            IndicatorGroupCode.FIN_SOURCE
    );

    private final ExternalRegionCollectorPort regionCollector;
    private final ExternalIndicatorCollectorPort indicatorCollector;
    private final RegionPersistencePort regionPersistence;
    private final PeriodPersistencePort periodPersistence;
    private final IndicatorPersistencePort indicatorPersistence;
    private final IndicatorYearEntryPersistencePort indicatorYearEntryPersistence;

    public ReferenceSyncService(
            ExternalRegionCollectorPort regionCollector,
            ExternalIndicatorCollectorPort indicatorCollector,
            RegionPersistencePort regionPersistence,
            PeriodPersistencePort periodPersistence,
            IndicatorPersistencePort indicatorPersistence,
            IndicatorYearEntryPersistencePort indicatorYearEntryPersistence
    ) {
        this.regionCollector = Objects.requireNonNull(regionCollector);
        this.indicatorCollector = Objects.requireNonNull(indicatorCollector);
        this.regionPersistence = Objects.requireNonNull(regionPersistence);
        this.periodPersistence = Objects.requireNonNull(periodPersistence);
        this.indicatorPersistence = Objects.requireNonNull(indicatorPersistence);
        this.indicatorYearEntryPersistence = Objects.requireNonNull(indicatorYearEntryPersistence);
    }

    private static <T> BinaryOperator<T> keepFirst() {
        return (left, right) -> left;
    }

    @Override
    public ReferenceSyncResultDto syncRegionsIfNecessary() {
        if (regionPersistence.existsAny()) {
            return ReferenceSyncResultDto.empty();
        }
        return syncRegions();
    }

    @Override
    public ReferenceSyncResultDto syncRegions() {
        List<ExternalRegionRow> rows = regionCollector.collectRegions();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        Set<String> seenCodes = new HashSet<>();

        for (ExternalRegionRow row : rows) {
            if (row == null || row.externalCode() == null || row.externalCode().isBlank()) {
                skipped++;
                continue;
            }
            String code = SourceRegionCode.compose(row.sourceSystemCode(), row.externalCode());
            if (!seenCodes.add(code)) {
                skipped++;
                continue;
            }

            Optional<StaticRegionCatalog.Entry> catalogEntry = StaticRegionCatalog.findByName(row.name());
            if (catalogEntry.isEmpty()) {
                skipped++;
                continue;
            }

            Optional<Region> existing = regionPersistence.findByCode(code);
            Region region = new Region(
                    existing.map(Region::id).orElse(null),
                    code,
                    catalogEntry.get().name(),
                    catalogEntry.get().federalDistrictCode()
            );
            regionPersistence.save(region);
            if (existing.isPresent()) {
                updated++;
            } else {
                created++;
            }
        }

        return new ReferenceSyncResultDto(rows.size(), created, updated, skipped);
    }

    @Override
    public ReferenceSyncResultDto syncIndicators(SyncIndicatorsCommand command) {
        validateIndicatorsCommand(command);

        if (command.groupCode() != null) {
            return syncIndicatorGroup(command.groupCode(), command.year());
        }

        ReferenceSyncResultDto result = ReferenceSyncResultDto.empty();
        for (IndicatorGroupCode groupCode : DEFAULT_GROUPS) {
            result = result.plus(syncIndicatorGroup(groupCode, command.year()));
        }
        return result;
    }

    private void validateIndicatorsCommand(SyncIndicatorsCommand command) {
        Objects.requireNonNull(command, "Indicators sync command must not be null");
        Objects.requireNonNull(command.year(), "Indicators sync year must not be null");
    }

    private ReferenceSyncResultDto syncIndicatorGroup(IndicatorGroupCode groupCode, int year) {
        Period yearPeriod = periodPersistence.getOrCreateYear(year);
        List<ExternalIndicatorRow> rows = indicatorCollector.collectIndicators(groupCode, year).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ExternalIndicatorRow::sortOrder))
                .toList();

        validateUniqueNaturalKeys(rows);

        Map<String, Indicator> indicatorsByIdentity = indicatorPersistence.findAllByGroup(groupCode).stream()
                .collect(Collectors.toMap(indicator ->
                                indicatorIdentity(indicator.name(), indicator.indicatorGroupCode()),
                        Function.identity(),
                        keepFirst(),
                        LinkedHashMap::new));

        List<Indicator> indicatorsToCreate = rows.stream()
                .filter(row -> row.name() != null && !row.name().isBlank())
                .map(row -> new Indicator(null, row.name(), row.groupCode()))
                .filter(indicator -> !indicatorsByIdentity.containsKey(indicatorIdentity(indicator.name(), indicator.indicatorGroupCode())))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(indicator ->
                                        indicatorIdentity(indicator.name(), indicator.indicatorGroupCode()),
                                Function.identity(),
                                keepFirst(),
                                LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));

        if (!indicatorsToCreate.isEmpty()) {
            for (Indicator saved : indicatorPersistence.saveAll(indicatorsToCreate)) {
                indicatorsByIdentity.put(indicatorIdentity(saved.name(), saved.indicatorGroupCode()), saved);
            }
        }

        Map<Long, Indicator> indicatorsById = indicatorsByIdentity.values().stream()
                .collect(Collectors.toMap(Indicator::id, Function.identity(), keepFirst(), LinkedHashMap::new));
        Set<Long> groupIndicatorIds = indicatorsById.keySet();
        Map<EntryIdentity, IndicatorYearEntry> existingEntries = indicatorYearEntryPersistence.findAllByPeriodId(yearPeriod.id()).stream()
                .filter(entry -> groupIndicatorIds.contains(entry.indicatorId()))
                .collect(Collectors.toMap(
                        entry -> new EntryIdentity(entry.indicatorId(), entry.parentIndicatorYearEntryId()),
                        Function.identity(),
                        keepFirst(),
                        LinkedHashMap::new
                ));

        Map<String, IndicatorYearEntry> entryByNaturalKey = new LinkedHashMap<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        Map<Integer, List<ExternalIndicatorRow>> rowsByLevel = rows.stream()
                .filter(row -> row.name() != null && !row.name().isBlank())
                .collect(Collectors.groupingBy(ExternalIndicatorRow::level, TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<ExternalIndicatorRow>> levelBucket : rowsByLevel.entrySet()) {
            List<IndicatorYearEntry> entriesToSave = new ArrayList<>();
            List<ExternalIndicatorRow> correspondingRows = new ArrayList<>();

            for (ExternalIndicatorRow row : levelBucket.getValue()) {
                Indicator indicator = indicatorsByIdentity.get(indicatorIdentity(row.name(), row.groupCode()));
                if (indicator == null) {
                    skipped++;
                    continue;
                }

                Long parentId = null;
                if (row.parentNaturalKey() != null && !row.parentNaturalKey().isBlank()) {
                    IndicatorYearEntry parent = entryByNaturalKey.get(row.parentNaturalKey());
                    if (parent == null) {
                        throw new IllegalStateException("Parent indicator was not synced before child: " + row.parentNaturalKey());
                    }
                    parentId = parent.id();
                }

                EntryIdentity identity = new EntryIdentity(indicator.id(), parentId);
                IndicatorYearEntry existing = existingEntries.get(identity);
                IndicatorYearEntry entry = new IndicatorYearEntry(
                        existing == null ? null : existing.id(),
                        yearPeriod.id(),
                        indicator.id(),
                        parentId,
                        row.level(),
                        row.sortOrder(),
                        row.hasChildren()
                );
                entriesToSave.add(entry);
                correspondingRows.add(row);
                if (existing == null) {
                    created++;
                } else {
                    updated++;
                }
            }

            List<IndicatorYearEntry> savedEntries = indicatorYearEntryPersistence.saveAll(entriesToSave);
            for (int i = 0; i < savedEntries.size(); i++) {
                IndicatorYearEntry saved = savedEntries.get(i);
                ExternalIndicatorRow row = correspondingRows.get(i);
                entryByNaturalKey.put(row.naturalKey(), saved);
                existingEntries.put(new EntryIdentity(saved.indicatorId(), saved.parentIndicatorYearEntryId()), saved);
            }
        }
        return new ReferenceSyncResultDto(rows.size(), created, updated, skipped);
    }

    private void validateUniqueNaturalKeys(List<ExternalIndicatorRow> rows) {
        Map<String, Long> countByKey = rows.stream()
                .filter(row -> row != null && row.naturalKey() != null)
                .collect(Collectors.groupingBy(ExternalIndicatorRow::naturalKey, Collectors.counting()));
        countByKey.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalStateException("Duplicate external indicator key: " + entry.getKey());
                });
    }

    private String indicatorIdentity(String name, IndicatorGroupCode groupCode) {
        return TextNormalizer.normalize(name) + "|" + groupCode;
    }

    private record EntryIdentity(Long indicatorId, Long parentIndicatorYearEntryId) {
    }
}
