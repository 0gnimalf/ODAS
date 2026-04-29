package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ObservationPersistencePort {

    Observation save(Observation observation);

    Observation upsertCurrent(Observation observation);

    int upsertCurrentBatch(Collection<Observation> observations);

    Optional<Observation> findCurrent(
            Long regionId,
            Long indicatorYearEntryId,
            Long periodId,
            ObservationValueKind valueKind
    );

    Set<Long> findRegionIdsWithCompleteCurrentObservations(
            Collection<Long> regionIds,
            Long indicatorYearEntryId,
            Collection<Long> periodIds,
            ObservationValueKind valueKind
    );
}
