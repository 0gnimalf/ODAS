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
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.IndicatorYearEntry;
import Ogni.ODAS.domain.model.Period;
import Ogni.ODAS.domain.model.Region;

import java.util.*;
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

    @Override
    public ReferenceSyncResultDto syncRegionsIfNecessary() {
        if (!regionPersistence.findAll().isEmpty()) {
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
                .sorted(Comparator.comparingInt(ExternalIndicatorRow::sortOrder))
                .toList();

        validateUniqueNaturalKeys(rows);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        Map<String, IndicatorYearEntry> entryByNaturalKey = new HashMap<>();

        for (ExternalIndicatorRow row : rows) {
            if (row == null || row.name() == null || row.name().isBlank()) {
                skipped++;
                continue;
            }

            Indicator indicator = indicatorPersistence.findByNameAndGroup(row.name(), row.groupCode())
                    .orElseGet(() -> indicatorPersistence.save(new Indicator(null, row.name(), row.groupCode())));

            Long parentId = null;
            if (row.parentNaturalKey() != null && !row.parentNaturalKey().isBlank()) {
                IndicatorYearEntry parent = entryByNaturalKey.get(row.parentNaturalKey());
                if (parent == null) {
                    throw new IllegalStateException("Parent indicator was not synced before child: " + row.parentNaturalKey());
                }
                parentId = parent.id();
            }

            Optional<IndicatorYearEntry> existing = indicatorYearEntryPersistence.findByIndicatorIdAndPeriodIdAndParentId(
                    indicator.id(),
                    yearPeriod.id(),
                    parentId
            );
            IndicatorYearEntry entry = new IndicatorYearEntry(
                    existing.map(IndicatorYearEntry::id).orElse(null),
                    yearPeriod.id(),
                    indicator.id(),
                    parentId,
                    row.level(),
                    row.sortOrder(),
                    row.hasChildren()
            );
            IndicatorYearEntry saved = indicatorYearEntryPersistence.save(entry);
            entryByNaturalKey.put(row.naturalKey(), saved);
            if (existing.isPresent()) {
                System.out.println("Duplicate indicator: " + entry.id());
                updated++;
            } else {
                created++;
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
}
