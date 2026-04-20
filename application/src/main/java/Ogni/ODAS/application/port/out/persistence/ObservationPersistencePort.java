package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;

import java.util.Collection;
import java.util.Optional;

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
}
