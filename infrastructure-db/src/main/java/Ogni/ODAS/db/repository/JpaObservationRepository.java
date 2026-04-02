package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.ObservationEntity;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaObservationRepository extends JpaRepository<ObservationEntity, Long> {

    List<ObservationEntity> findAllByRegionCodeAndIndicatorIndicatorGroupCodeAndIndicatorCodeAndReportingPeriodYearAndReportingPeriodMonth(
            String regionCode,
            IndicatorGroupCode indicatorGroupCode,
            String indicatorCode,
            Integer year,
            Integer month
    );

    Optional<ObservationEntity> findByDatasetVersionIdAndRegionCodeAndIndicatorIndicatorGroupCodeAndIndicatorCodeAndReportingPeriodYearAndReportingPeriodMonthAndValueKind(
            Long datasetVersionId,
            String regionCode,
            IndicatorGroupCode indicatorGroupCode,
            String indicatorCode,
            Integer year,
            Integer month,
            ObservationValueKind valueKind
    );
}
