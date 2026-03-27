package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.port.out.RegionDictionaryRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.RegionEntityMapper;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.domain.model.Region;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RegionDictionaryRepositoryAdapter implements RegionDictionaryRepositoryPort {

    private final JpaRegionRepository repository;

    public RegionDictionaryRepositoryAdapter(JpaRegionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return repository.findByCode(code).map(RegionEntityMapper::toDomain);
    }

    @Override
    public Optional<Region> findByNameIgnoreCase(String name) {
        return repository.findByNameIgnoreCase(name).map(RegionEntityMapper::toDomain);
    }
}
