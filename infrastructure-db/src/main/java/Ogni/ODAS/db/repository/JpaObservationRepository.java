package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaObservationRepository extends JpaRepository<ObservationEntity, Long> {

    List<ObservationEntity> findAllByRegion_CodeAndIndicator_CodeAndReportingPeriod_YearAndReportingPeriod_Month(
            String regionCode,
            String indicatorCode,
            Integer year,
            Integer month
    );
}
