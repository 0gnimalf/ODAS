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
        int indicatorsProcessed = upsertIndicators(indicators);
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
    public List<Indicator> getIndicators(IndicatorGroupCode groupCode) {
        return indicatorRepositoryPort.findAllByGroupCode(groupCode);
    }

    private void validateIndicatorsCommand(SyncIndicatorsCommand command) {
        Objects.requireNonNull(command, "Indicators sync command must not be null");
        Objects.requireNonNull(command.year(), "Indicators sync year must not be null");
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

    private int upsertIndicators(List<CollectedIndicatorDto> indicators) {
        int processed = 0;
        Set<String> sectionKeys = indicators.stream()
                .filter(indicator -> indicator.parentCode() != null && !indicator.parentCode().isBlank())
                .map(indicator -> indicator.groupCode().name() + "::" + indicator.parentCode())
                .collect(Collectors.toSet());

        List<CollectedIndicatorDto> sorted = indicators.stream()
                .sorted(Comparator
                        .comparing(CollectedIndicatorDto::groupCode)
                        .thenComparing(dto -> dto.level() == null ? Integer.MAX_VALUE : dto.level())
                        .thenComparing(dto -> dto.sortOrder() == null ? Integer.MAX_VALUE : dto.sortOrder())
                        .thenComparing(CollectedIndicatorDto::code))
                .toList();

        for (CollectedIndicatorDto indicator : sorted) {
            Optional<Indicator> existing = indicatorRepositoryPort.findByCodeAndGroupCode(indicator.code(), indicator.groupCode());
            Long parentId = null;
            if (indicator.parentCode() != null && !indicator.parentCode().isBlank()) {
                parentId = indicatorRepositoryPort.findByCodeAndGroupCode(indicator.parentCode(), indicator.groupCode())
                        .map(Indicator::id)
                        .orElse(null);
            }

            indicatorRepositoryPort.save(new Indicator(
                    existing.map(Indicator::id).orElse(null),
                    indicator.code(),
                    indicator.name(),
                    indicator.groupCode(),
                    parentId,
                    indicator.level(),
                    indicator.sortOrder(),
                    indicator.section() || sectionKeys.contains(indicator.groupCode().name() + "::" + indicator.code())
            ));
            processed++;
        }
        return processed;
    }
}
