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

    @Override
    public ObservationCollectionResultDto collectMonthlyObservations(CollectObservationsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        referenceSyncUseCase.syncRegionsIfNecessary();
        ensureIndicators(command.groupCode(), command.year());

        Period period = periodPersistence.getOrCreateMonth(command.year(), command.month());
        Period yearPeriod = periodPersistence.getOrCreateYear(command.year());
        Map<String, Region> regionsByExternalCode = loadRegionsByExternalCode();
        List<IndicatorEntryCandidate> orderedEntries = loadOrderedEntries(command.groupCode(), yearPeriod.id());
        List<ExternalRegionRef> externalRegions = regionsByExternalCode.values().stream().map(region -> new ExternalRegionRef(SourceSystemCode.IMINFIN, SourceRegionCode.externalPart(region.code(), SourceSystemCode.IMINFIN), region.name())).limit(2) // вместо первых двух загружать в соответствии с переданным списком
                .toList();

        int payloadCount = 0;
        int received = 0;
        int saved = 0;
        int skipped = 0;

        for (ExternalDatasetPayload payload : observationCollector.collectObservations(command.groupCode(), command.year(), command.month(), externalRegions)) {
            payloadCount++;
            DatasetVersion version = datasetVersionPersistence.findByIdentity(payload.sourceSystemCode(), payload.externalTitle(), payload.externalDateModified()).orElseGet(() -> datasetVersionPersistence.save(new DatasetVersion(null, payload.sourceSystemCode(), payload.externalTitle(), payload.externalDateModified())));
            DatasetCollection collection = datasetCollectionPersistence.save(new DatasetCollection(null, version.id(), OffsetDateTime.now(ZoneOffset.UTC), payload.request(), payload.rawData()));

            Map<String, OrderedIndicatorEntryMatcher> matchers = new HashMap<>();

            for (ExternalObservationRow row : payload.observations()) {
                received++;
                Region region = regionsByExternalCode.get(row.regionExternalCode());

                if (region == null || row.indicatorName() == null || row.value() == null || row.valueKind() == null) {
                    skipped++;
                    continue;
                }

                String matcherKey = row.regionExternalCode() + "|" + row.valueKind();
                OrderedIndicatorEntryMatcher matcher = matchers.computeIfAbsent(matcherKey, ignored -> new OrderedIndicatorEntryMatcher(orderedEntries));
                IndicatorYearEntry entry = matcher.next(row.indicatorName()).orElse(null);
                if (entry == null) {
                    skipped++;
                    continue;
                }
                observationPersistence.upsertCurrent(new Observation(null, collection.id(), region.id(), entry.id(), period.id(), row.valueKind(), row.value()));
                saved++;
            }
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

    private Map<String, Region> loadRegionsByExternalCode() {
        Map<String, Region> result = new LinkedHashMap<>();
        for (Region region : regionPersistence.findAll()) {
            try {
                String externalCode = SourceRegionCode.externalPart(region.code(), SourceSystemCode.IMINFIN);
                result.putIfAbsent(externalCode, region);
            } catch (IllegalArgumentException ignored) {
                // Region belongs to another external source; it is ignored for iMinfin collection.
            }
        }
        return result;
    }

    private List<IndicatorEntryCandidate> loadOrderedEntries(IndicatorGroupCode groupCode, Long yearPeriodId) {
        Map<Long, Indicator> indicatorsById = indicatorPersistence.findAllByGroup(groupCode).stream().collect(Collectors.toMap(Indicator::id, Function.identity()));

        return indicatorYearEntryPersistence.findAllByPeriodId(yearPeriodId).stream().filter(entry -> indicatorsById.containsKey(entry.indicatorId())).sorted(Comparator.comparing(IndicatorYearEntry::sortOrder).thenComparing(IndicatorYearEntry::id)).map(entry -> {
            Indicator indicator = indicatorsById.get(entry.indicatorId());

            return new IndicatorEntryCandidate(entry, TextNormalizer.normalize(indicator.name()));
        }).toList();
    }

    private record IndicatorEntryCandidate(IndicatorYearEntry entry, String normalizedName) {
    }

    private static final class OrderedIndicatorEntryMatcher {

        private final List<IndicatorEntryCandidate> candidates;
        private final boolean[] used;
        private int cursor = 0;

        private OrderedIndicatorEntryMatcher(List<IndicatorEntryCandidate> candidates) {
            this.candidates = candidates;
            this.used = new boolean[candidates.size()];
        }

        private Optional<IndicatorYearEntry> next(String indicatorName) {
            String normalizedName = TextNormalizer.normalize(indicatorName);
            for (int i = cursor; i < candidates.size(); i++) {
                if (used[i]) {
                    continue;
                }
                IndicatorEntryCandidate candidate = candidates.get(i);
                if (!candidate.normalizedName().equals(normalizedName)) {
                    continue;
                }
                used[i] = true;
                cursor = i + 1;
                return Optional.of(candidate.entry());
            }
            return Optional.empty();
        }
    }
}
