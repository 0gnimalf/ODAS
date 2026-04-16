package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.ObservationEntity;
import Ogni.ODAS.domain.model.Observation;

public final class ObservationEntityMapper {

    private ObservationEntityMapper() {
    }

    public static ObservationEntity toEntity(Observation observation) {
        if (observation == null) {
            return null;
        }
        return new ObservationEntity(
                observation.id(),
                observation.datasetCollectionId(),
                observation.regionId(),
                observation.indicatorYearEntryId(),
                observation.periodId(),
                observation.observationValueKind(),
                observation.value()
        );
    }

    public static Observation toDomain(ObservationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Observation(
                entity.getId(),
                entity.getDatasetCollectionId(),
                entity.getRegionId(),
                entity.getIndicatorYearEntryId(),
                entity.getPeriodId(),
                entity.getObservationValueKind(),
                entity.getValue()
        );
    }
}
