package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.IndicatorPersistencePort;
import Ogni.ODAS.db.mapper.IndicatorEntityMapper;
import Ogni.ODAS.db.repository.IndicatorJpaRepository;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;

import java.util.List;
import java.util.Optional;

public class IndicatorPersistenceAdapter implements IndicatorPersistencePort {

    private final IndicatorJpaRepository repository;

    public IndicatorPersistenceAdapter(IndicatorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Indicator save(Indicator indicator) {
        return IndicatorEntityMapper.toDomain(repository.save(IndicatorEntityMapper.toEntity(indicator)));
    }

    @Override
    public Optional<Indicator> findByNameAndGroup(String name, IndicatorGroupCode groupCode) {
        return repository.findByNameAndIndicatorGroupCode(name, groupCode).map(IndicatorEntityMapper::toDomain);
    }

    @Override
    public List<Indicator> findAllByGroup(IndicatorGroupCode groupCode) {
        return repository.findAllByIndicatorGroupCodeOrderByNameAsc(groupCode).stream()
                .map(IndicatorEntityMapper::toDomain)
                .toList();
    }
}
