package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.validation.DomainPreconditions;

import java.math.BigDecimal;

public record Observation(
        Long id,
        Long datasetCollectionId,
        Long regionId,
        Long indicatorYearEntryId,
        Long periodId,
        ObservationValueKind observationValueKind,
        BigDecimal value
) {
    public Observation {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.validateId(datasetCollectionId, "datasetCollectionId");
        DomainPreconditions.validateId(regionId, "regionId");
        DomainPreconditions.validateId(indicatorYearEntryId, "indicatorYearEntryId");
        DomainPreconditions.validateId(periodId, "periodId");
        DomainPreconditions.notNull(observationValueKind, "valueKind");
        DomainPreconditions.notNull(value, "value");
    }
}
