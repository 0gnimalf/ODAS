package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.IndicatorYearEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicatorYearEntryJpaRepository extends JpaRepository<IndicatorYearEntryEntity, Long> {

    Optional<IndicatorYearEntryEntity> findByIndicatorIdAndPeriodIdAndParentIndicatorYearEntryId(Long indicatorId, Long periodId, Long parentId);

    List<IndicatorYearEntryEntity> findAllByPeriodIdOrderBySortOrderAsc(Long periodId);

    List<IndicatorYearEntryEntity> findAllByParentIndicatorYearEntryIdOrderBySortOrderAsc(Long parentIndicatorYearEntryId);

    List<IndicatorYearEntryEntity> findAllByPeriodIdAndLevelOrderBySortOrderAsc(Long periodId, Integer level);
}
