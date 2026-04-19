package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.IndicatorYearEntryPersistencePort;
import Ogni.ODAS.db.mapper.IndicatorYearEntryEntityMapper;
import Ogni.ODAS.db.repository.IndicatorYearEntryJpaRepository;
import Ogni.ODAS.domain.model.IndicatorYearEntry;

import java.util.List;
import java.util.Optional;

public class IndicatorYearEntryPersistenceAdapter implements IndicatorYearEntryPersistencePort {

    private final IndicatorYearEntryJpaRepository repository;

    public IndicatorYearEntryPersistenceAdapter(IndicatorYearEntryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public IndicatorYearEntry save(IndicatorYearEntry entry) {
        return IndicatorYearEntryEntityMapper.toDomain(repository.save(IndicatorYearEntryEntityMapper.toEntity(entry)));
    }

    @Override
    public Optional<IndicatorYearEntry> findByIndicatorIdAndPeriodIdAndParentId(Long indicatorId, Long periodId, Long parentId) {
        return repository.findByIndicatorIdAndPeriodIdAndParentIndicatorYearEntryId(indicatorId, periodId, parentId)
                .map(IndicatorYearEntryEntityMapper::toDomain);
    }

    @Override
    public List<IndicatorYearEntry> findAllByPeriodId(Long periodId) {
        return repository.findAllByPeriodIdOrderBySortOrderAsc(periodId).stream()
                .map(IndicatorYearEntryEntityMapper::toDomain)
                .toList();
    }
}
