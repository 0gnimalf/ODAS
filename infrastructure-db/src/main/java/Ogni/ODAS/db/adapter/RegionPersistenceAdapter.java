package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.RegionPersistencePort;
import Ogni.ODAS.db.mapper.RegionEntityMapper;
import Ogni.ODAS.db.repository.RegionJpaRepository;
import Ogni.ODAS.domain.model.Region;

import java.util.List;
import java.util.Optional;

public class RegionPersistenceAdapter implements RegionPersistencePort {

    private final RegionJpaRepository repository;

    public RegionPersistenceAdapter(RegionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Region save(Region region) {
        return RegionEntityMapper.toDomain(repository.save(RegionEntityMapper.toEntity(region)));
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return repository.findByCode(code).map(RegionEntityMapper::toDomain);
    }

    @Override
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    public List<Region> findAll() {
        return repository.findAll().stream().map(RegionEntityMapper::toDomain).toList();
    }
}
