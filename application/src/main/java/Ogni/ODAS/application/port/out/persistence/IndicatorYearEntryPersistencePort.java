package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.model.IndicatorYearEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IndicatorYearEntryPersistencePort {

    IndicatorYearEntry save(IndicatorYearEntry entry);

    List<IndicatorYearEntry> saveAll(Collection<IndicatorYearEntry> entries);

    Optional<IndicatorYearEntry> findByIndicatorIdAndPeriodIdAndParentId(Long indicatorId, Long periodId, Long parentId);

    List<IndicatorYearEntry> findAllByPeriodId(Long periodId);
}
