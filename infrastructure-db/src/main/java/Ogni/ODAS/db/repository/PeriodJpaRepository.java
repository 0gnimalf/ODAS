package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.PeriodEntity;
import Ogni.ODAS.domain.enumtype.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodJpaRepository extends JpaRepository<PeriodEntity, Long> {

    Optional<PeriodEntity> findByPeriodTypeAndYearAndMonthAndQuarter(
            PeriodType periodType,
            Integer year,
            Integer month,
            Integer quarter
    );

    List<PeriodEntity> findAllByYear(Integer year);

    List<PeriodEntity> findAllByPeriodTypeOrderByYearAscMonthAscQuarterAsc(PeriodType periodType);
}
