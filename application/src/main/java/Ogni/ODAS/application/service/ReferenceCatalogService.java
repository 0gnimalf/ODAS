package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceCatalogUseCase;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.IndicatorYearEntry;
import Ogni.ODAS.domain.model.Region;

import java.util.*;
import java.util.stream.Collectors;

public class ReferenceCatalogService implements ReferenceCatalogUseCase {

    private final ExternalReferenceCollectorPort externalReferenceCollectorPort;
    private final RegionRepositoryPort regionRepositoryPort;
    private final IndicatorRepositoryPort indicatorRepositoryPort;

    public ReferenceCatalogService(
            ExternalReferenceCollectorPort externalReferenceCollectorPort,
            RegionRepositoryPort regionRepositoryPort,
            IndicatorRepositoryPort indicatorRepositoryPort
    ) {
        this.externalReferenceCollectorPort = externalReferenceCollectorPort;
        this.regionRepositoryPort = regionRepositoryPort;
        this.indicatorRepositoryPort = indicatorRepositoryPort;
    }

    @Override
    public ReferenceSyncResultDto syncRegions() {
        List<CollectedRegionDto> regions = externalReferenceCollectorPort.collectRegions();
        int regionsProcessed = upsertRegions(regions);
        return new ReferenceSyncResultDto(regionsProcessed, 0, 0, 0, 0, 0);
    }

    @Override
    public ReferenceSyncResultDto syncIndicators(SyncIndicatorsCommand command) {
        validateIndicatorsCommand(command);

        List<CollectedIndicatorDto> indicators = externalReferenceCollectorPort.collectIndicators(command);
        Set<IndicatorGroupCode> targetGroups = resolveTargetGroups(command);
        int indicatorsProcessed = replaceIndicatorYearEntries(indicators, command.year(), targetGroups);
        Map<IndicatorGroupCode, Integer> counts = countIndicatorsByGroup(indicators);

        return new ReferenceSyncResultDto(
                0,
                indicatorsProcessed,
                counts.getOrDefault(IndicatorGroupCode.INCOME, 0),
                counts.getOrDefault(IndicatorGroupCode.OUTCOME, 0),
                counts.getOrDefault(IndicatorGroupCode.CREDIT, 0),
                counts.getOrDefault(IndicatorGroupCode.FIN_SOURCE, 0)
        );
    }

    @Override
    public List<Region> getRegions() {
        return regionRepositoryPort.findAll().stream()
                .sorted(Comparator.comparing(Region::name))
                .toList();
    }

    @Override
    public List<IndicatorYearEntry> getIndicators(IndicatorGroupCode groupCode, Integer year) {
        Objects.requireNonNull(groupCode, "Indicator group code must not be null");
        Objects.requireNonNull(year, "Indicator year must not be null");
        return indicatorRepositoryPort.findAllByGroupCodeAndYear(groupCode, year);
    }

    private void validateIndicatorsCommand(SyncIndicatorsCommand command) {
        Objects.requireNonNull(command, "Indicators sync command must not be null");
        Objects.requireNonNull(command.year(), "Indicators sync year must not be null");
    }

    private Set<IndicatorGroupCode> resolveTargetGroups(SyncIndicatorsCommand command) {
        if (command.groupCode() != null) {
            return EnumSet.of(command.groupCode());
        }
        return EnumSet.of(
                IndicatorGroupCode.INCOME,
                IndicatorGroupCode.OUTCOME,
                IndicatorGroupCode.CREDIT,
                IndicatorGroupCode.FIN_SOURCE
        );
    }

    private Map<IndicatorGroupCode, Integer> countIndicatorsByGroup(List<CollectedIndicatorDto> indicators) {
        Map<IndicatorGroupCode, Integer> counts = new EnumMap<>(IndicatorGroupCode.class);
        for (CollectedIndicatorDto indicator : indicators) {
            counts.merge(indicator.groupCode(), 1, Integer::sum);
        }
        return counts;
    }

    private int upsertRegions(List<CollectedRegionDto> regions) {
        int processed = 0;
        for (CollectedRegionDto region : regions) {
            Optional<Region> existing = regionRepositoryPort.findByCode(region.code());
            regionRepositoryPort.save(new Region(
                    existing.map(Region::id).orElse(null),
                    region.code(),
                    region.name(),
                    region.federalDistrictCode()
            ));
            processed++;
        }
        return processed;
    }

    private int replaceIndicatorYearEntries(
            List<CollectedIndicatorDto> indicators,
            Integer year,
            Set<IndicatorGroupCode> targetGroups
    ) {
        Map<IndicatorKey, Indicator> indicatorsByKey = indicators.isEmpty()
                ? Map.of()
                : upsertIndicatorMasters(indicators);
        Map<IndicatorGroupCode, List<IndicatorYearEntry>> entriesByGroup = indicators.isEmpty()
                ? Map.of()
                : buildYearEntries(indicators, indicatorsByKey, year);

        for (IndicatorGroupCode groupCode : targetGroups) {
            indicatorRepositoryPort.replaceYearEntries(
                    groupCode,
                    year,
                    entriesByGroup.getOrDefault(groupCode, List.of())
            );
        }
        return indicators.size();
    }

    private Map<IndicatorKey, Indicator> upsertIndicatorMasters(List<CollectedIndicatorDto> indicators) {
        Map<IndicatorKey, Indicator> result = new LinkedHashMap<>();

        indicators.stream()
                .map(dto -> new IndicatorKey(dto.groupCode(), dto.code()))
                .distinct()
                .sorted(Comparator
                        .comparing(IndicatorKey::groupCode)
                        .thenComparing(IndicatorKey::code))
                .forEach(key -> {
                    Indicator saved = indicatorRepositoryPort.findByCodeAndGroupCode(key.code(), key.groupCode())
                            .orElseGet(() -> indicatorRepositoryPort.save(new Indicator(null, key.code(), key.groupCode())));
                    result.put(key, saved);
                });

        return result;
    }

    private Map<IndicatorGroupCode, List<IndicatorYearEntry>> buildYearEntries(
            List<CollectedIndicatorDto> indicators,
            Map<IndicatorKey, Indicator> indicatorsByKey,
            Integer year
    ) {
        Set<IndicatorKey> sectionKeys = indicators.stream()
                .filter(indicator -> indicator.parentCode() != null && !indicator.parentCode().isBlank())
                .map(indicator -> new IndicatorKey(indicator.groupCode(), indicator.parentCode()))
                .collect(Collectors.toSet());

        Map<IndicatorGroupCode, List<CollectedIndicatorDto>> sourceByGroup = indicators.stream()
                .collect(Collectors.groupingBy(CollectedIndicatorDto::groupCode, () -> new EnumMap<>(IndicatorGroupCode.class), Collectors.toList()));

        Map<IndicatorGroupCode, List<IndicatorYearEntry>> result = new EnumMap<>(IndicatorGroupCode.class);
        for (Map.Entry<IndicatorGroupCode, List<CollectedIndicatorDto>> entry : sourceByGroup.entrySet()) {
            List<IndicatorYearEntry> yearEntries = entry.getValue().stream()
                    .sorted(Comparator
                            .comparing(CollectedIndicatorDto::groupCode)
                            .thenComparing(dto -> dto.level() == null ? Integer.MAX_VALUE : dto.level())
                            .thenComparing(dto -> dto.sortOrder() == null ? Integer.MAX_VALUE : dto.sortOrder())
                            .thenComparing(CollectedIndicatorDto::code))
                    .map(dto -> toYearEntry(dto, indicatorsByKey, sectionKeys, year))
                    .toList();
            result.put(entry.getKey(), yearEntries);
        }
        return result;
    }

    private IndicatorYearEntry toYearEntry(
            CollectedIndicatorDto indicator,
            Map<IndicatorKey, Indicator> indicatorsByKey,
            Set<IndicatorKey> sectionKeys,
            Integer year
    ) {
        IndicatorKey key = new IndicatorKey(indicator.groupCode(), indicator.code());
        Indicator masterIndicator = requireIndicator(indicatorsByKey, key);

        Long parentIndicatorId = null;
        if (indicator.parentCode() != null && !indicator.parentCode().isBlank()) {
            IndicatorKey parentKey = new IndicatorKey(indicator.groupCode(), indicator.parentCode());
            parentIndicatorId = requireIndicator(indicatorsByKey, parentKey).id();
        }

        return new IndicatorYearEntry(
                null,
                masterIndicator.id(),
                masterIndicator.code(),
                masterIndicator.groupCode(),
                year,
                indicator.name(),
                parentIndicatorId,
                indicator.level(),
                indicator.sortOrder(),
                indicator.section() || sectionKeys.contains(key)
        );
    }

    private Indicator requireIndicator(Map<IndicatorKey, Indicator> indicatorsByKey, IndicatorKey key) {
        Indicator indicator = indicatorsByKey.get(key);
        if (indicator == null) {
            throw new IllegalStateException("Indicator master not found for key: " + key.groupCode() + ":" + key.code());
        }
        return indicator;
    }

    private record IndicatorKey(IndicatorGroupCode groupCode, String code) {
    }
}
