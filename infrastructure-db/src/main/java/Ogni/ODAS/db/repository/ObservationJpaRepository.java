package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.ObservationEntity;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObservationJpaRepository extends JpaRepository<ObservationEntity, Long> {

    Optional<ObservationEntity> findByRegionIdAndIndicatorYearEntryIdAndPeriodIdAndObservationValueKind(
            Long regionId,
            Long indicatorYearEntryId,
            Long periodId,
            ObservationValueKind observationValueKind
    );

    List<ObservationEntity> findAllByRegionIdAndIndicatorYearEntryIdAndPeriodId(
            Long regionId,
            Long indicatorYearEntryId,
            Long periodId
    );

    List<ObservationEntity> findAllByRegionIdAndPeriodId(Long regionId, Long periodId);

    List<ObservationEntity> findAllByIndicatorYearEntryIdAndPeriodId(Long indicatorYearEntryId, Long periodId);

    List<ObservationEntity> findAllByPeriodId(Long periodId);

    List<ObservationEntity> findAllByDatasetCollectionId(Long datasetCollectionId);
}
