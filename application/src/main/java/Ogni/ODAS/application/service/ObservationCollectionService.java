package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.CollectObservationsCommand;
import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ExternalDatasetPayload;
import Ogni.ODAS.application.dto.ExternalObservationRow;
import Ogni.ODAS.application.dto.ExternalRegionRef;
import Ogni.ODAS.application.dto.ObservationCollectionResultDto;
import Ogni.ODAS.application.port.in.ObservationCollectionUseCase;
import Ogni.ODAS.application.port.in.ReferenceSyncUseCase;
import Ogni.ODAS.application.port.out.collector.ExternalObservationCollectorPort;
import Ogni.ODAS.application.port.out.persistence.*;
import Ogni.ODAS.application.support.SourceRegionCode;
import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObservationCollectionService implements ObservationCollectionUseCase {

    private final ReferenceSyncUseCase referenceSyncUseCase;
    private final ExternalObservationCollectorPort observationCollector;
    private final RegionPersistencePort regionPersistence;
    private final PeriodPersistencePort periodPersistence;
    private final IndicatorPersistencePort indicatorPersistence;
    private final IndicatorYearEntryPersistencePort indicatorYearEntryPersistence;
    private final DatasetVersionPersistencePort datasetVersionPersistence;
    private final DatasetCollectionPersistencePort datasetCollectionPersistence;
    private final ObservationPersistencePort observationPersistence;

    public ObservationCollectionService(ReferenceSyncUseCase referenceSyncUseCase, ExternalObservationCollectorPort observationCollector, RegionPersistencePort regionPersistence, PeriodPersistencePort periodPersistence, IndicatorPersistencePort indicatorPersistence, IndicatorYearEntryPersistencePort indicatorYearEntryPersistence, DatasetVersionPersistencePort datasetVersionPersistence, DatasetCollectionPersistencePort datasetCollectionPersistence, ObservationPersistencePort observationPersistence) {
        this.referenceSyncUseCase = Objects.requireNonNull(referenceSyncUseCase);
        this.observationCollector = Objects.requireNonNull(observationCollector);
        this.regionPersistence = Objects.requireNonNull(regionPersistence);
        this.periodPersistence = Objects.requireNonNull(periodPersistence);
        this.indicatorPersistence = Objects.requireNonNull(indicatorPersistence);
        this.indicatorYearEntryPersistence = Objects.requireNonNull(indicatorYearEntryPersistence);
        this.datasetVersionPersistence = Objects.requireNonNull(datasetVersionPersistence);
        this.datasetCollectionPersistence = Objects.requireNonNull(datasetCollectionPersistence);
        this.observationPersistence = Objects.requireNonNull(observationPersistence);
    }

    private static <T> java.util.function.BinaryOperator<T> keepFirst() {
        return (left, right) -> left;
    }

    @Override
    public ObservationCollectionResultDto collectMonthlyObservations(CollectObservationsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        referenceSyncUseCase.syncRegionsIfNecessary();
        ensureIndicators(command.groupCode(), command.year());

        Period period = periodPersistence.getOrCreateMonth(command.year(), command.month());
        Period yearPeriod = periodPersistence.getOrCreateYear(command.year());
        Map<String, Region> regionsByExternalCode = loadSelectedRegionsByExternalCode(command.regionIds());
        Map<IndicatorLookupKey, IndicatorYearEntry> entriesByNameAndParent = loadEntriesByNameAndParent(command.groupCode(), yearPeriod.id());
        List<ExternalRegionRef> externalRegions = regionsByExternalCode.values().stream()
                .map(region -> new ExternalRegionRef(
                        SourceSystemCode.IMINFIN,
                        SourceRegionCode.externalPart(region.code(), SourceSystemCode.IMINFIN),
                        region.name()
                ))
                .toList();

        int payloadCount = 0;
        int received = 0;
        int saved = 0;
        int skipped = 0;

        for (ExternalDatasetPayload payload : observationCollector.collectObservations(command.groupCode(), command.year(), command.month(), externalRegions)) {
            payloadCount++;
            DatasetVersion version = datasetVersionPersistence.findByIdentity(payload.sourceSystemCode(), payload.externalTitle(), payload.externalDateModified())
                    .orElseGet(() -> datasetVersionPersistence.save(new DatasetVersion(null, payload.sourceSystemCode(), payload.externalTitle(), payload.externalDateModified())));
            DatasetCollection collection = datasetCollectionPersistence.save(new DatasetCollection(null, version.id(), OffsetDateTime.now(ZoneOffset.UTC), payload.request(), payload.rawData()));

            Map<ObservationIdentity, Observation> observationsToSave = new LinkedHashMap<>();
            for (ExternalObservationRow row : payload.observations()) {
                received++;
                Region region = row == null ? null : regionsByExternalCode.get(row.regionExternalCode());

                if (region == null || row.indicatorName() == null || row.value() == null || row.valueKind() == null) {
                    skipped++;
                    continue;
                }

                IndicatorYearEntry entry = entriesByNameAndParent.get(IndicatorLookupKey.from(row.indicatorName(), row.parentIndicatorName()));
                if (entry == null) {
                    skipped++;
                    continue;
                }
                Observation observation = new Observation(
                        null,
                        collection.id(),
                        region.id(),
                        entry.id(),
                        period.id(),
                        row.valueKind(),
                        row.value()
                );
                observationsToSave.put(ObservationIdentity.from(observation), observation);
            }

            int batchSaved = observationPersistence.upsertCurrentBatch(observationsToSave.values());
            saved += batchSaved;
        }

        return new ObservationCollectionResultDto(payloadCount, received, saved, skipped);
    }

    private void ensureIndicators(IndicatorGroupCode groupCode, int year) {
        Period yearPeriod = periodPersistence.getOrCreateYear(year);
        Map<Long, Indicator> indicators = indicatorPersistence.findAllByGroup(groupCode).stream().collect(Collectors.toMap(Indicator::id, Function.identity()));
        boolean hasEntries = indicatorYearEntryPersistence.findAllByPeriodId(yearPeriod.id()).stream().anyMatch(entry -> indicators.containsKey(entry.indicatorId()));
        if (!hasEntries) {
            referenceSyncUseCase.syncIndicators(new SyncIndicatorsCommand(groupCode, year));
        }
    }

    private Map<String, Region> loadSelectedRegionsByExternalCode(List<Long> requestedRegionCodes) {
        Set<Long> requested = requestedRegionCodes == null ? Set.of() : new LinkedHashSet<>(requestedRegionCodes);
        Map<String, Region> result = new LinkedHashMap<>();
        for (Region region : regionPersistence.findAll()) {
            if (!requested.isEmpty() && !requested.contains(region.id())) {
                continue;
            }
            try {
                String externalCode = SourceRegionCode.externalPart(region.code(), SourceSystemCode.IMINFIN);
                result.putIfAbsent(externalCode, region);
            } catch (IllegalArgumentException ignored) {
                // Region belongs to another external source; it is ignored for iMinfin collection.
            }
        }
        return result;
    }

    private Map<IndicatorLookupKey, IndicatorYearEntry> loadEntriesByNameAndParent(IndicatorGroupCode groupCode, Long yearPeriodId) {
        Map<Long, Indicator> indicatorsById = indicatorPersistence.findAllByGroup(groupCode).stream()
                .collect(Collectors.toMap(Indicator::id, Function.identity()));
        Map<Long, IndicatorYearEntry> entriesById = indicatorYearEntryPersistence.findAllByPeriodId(yearPeriodId).stream()
                .filter(entry -> indicatorsById.containsKey(entry.indicatorId()))
                .collect(Collectors.toMap(
                        IndicatorYearEntry::id,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        Map<IndicatorLookupKey, IndicatorYearEntry> result = new LinkedHashMap<>();
        for (IndicatorYearEntry entry : entriesById.values()) {
            Indicator indicator = indicatorsById.get(entry.indicatorId());
            String parentName = null;
            if (entry.parentIndicatorYearEntryId() != null) {
                IndicatorYearEntry parentEntry = entriesById.get(entry.parentIndicatorYearEntryId());
                if (parentEntry != null) {
                    Indicator parentIndicator = indicatorsById.get(parentEntry.indicatorId());
                    parentName = parentIndicator == null ? null : parentIndicator.name();
                }
            }
            result.putIfAbsent(IndicatorLookupKey.from(indicator.name(), parentName), entry);
        }
        return result;
    }

    private record IndicatorLookupKey(String normalizedName, String normalizedParentName) {
        private static IndicatorLookupKey from(String name, String parentName) {
            return new IndicatorLookupKey(TextNormalizer.normalize(name), normalizeNullable(parentName));
        }

        private static String normalizeNullable(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = TextNormalizer.normalize(value);
            return normalized.isBlank() ? null : normalized;
        }
    }

    private record ObservationIdentity(Long regionId, Long indicatorYearEntryId, Long periodId,
                                       ObservationValueKind valueKind) {
        private static ObservationIdentity from(Observation observation) {
            return new ObservationIdentity(
                    observation.regionId(),
                    observation.indicatorYearEntryId(),
                    observation.periodId(),
                    observation.observationValueKind()
            );
        }
    }
}
