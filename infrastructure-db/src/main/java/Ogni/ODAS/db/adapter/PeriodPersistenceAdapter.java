package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.PeriodPersistencePort;
import Ogni.ODAS.db.mapper.PeriodEntityMapper;
import Ogni.ODAS.db.repository.PeriodJpaRepository;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.Period;

import java.util.Optional;

public class PeriodPersistenceAdapter implements PeriodPersistencePort {

    private final PeriodJpaRepository repository;

    public PeriodPersistenceAdapter(PeriodJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Period save(Period period) {
        return PeriodEntityMapper.toDomain(repository.save(PeriodEntityMapper.toEntity(period)));
    }

    @Override
    public Optional<Period> findByIdentity(PeriodType periodType, Integer year, Integer month, Integer quarter) {
        return repository.findByPeriodTypeAndYearAndMonthAndQuarter(periodType, year, month, quarter)
                .map(PeriodEntityMapper::toDomain);
    }
}
