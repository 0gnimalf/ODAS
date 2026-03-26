package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.domain.enumtype.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaReportingPeriodRepository extends JpaRepository<ReportingPeriodEntity, Long> {

    Optional<ReportingPeriodEntity> findByPeriodTypeAndYearAndMonthAndQuarter(
            PeriodType periodType,
            Integer year,
            Integer month,
            Integer quarter
    );
}
