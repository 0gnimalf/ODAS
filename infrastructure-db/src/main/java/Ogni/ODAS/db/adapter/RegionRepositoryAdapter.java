package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.db.mapper.RegionEntityMapper;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.domain.model.Region;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RegionRepositoryAdapter implements RegionRepositoryPort {

    private final JpaRegionRepository repository;

    public RegionRepositoryAdapter(JpaRegionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Region save(Region region) {
        var saved = repository.save(RegionEntityMapper.toEntity(region));
        return RegionEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return repository.findByCode(code)
                .map(RegionEntityMapper::toDomain);
    }

    @Override
    public List<Region> saveAll(List<Region> regions) {
        if (regions.isEmpty()) {
            return List.of();
        }

        var entities = regions.stream()
                .map(RegionEntityMapper::toEntity)
                .toList();

        return repository.saveAll(entities).stream()
                .map(RegionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Region> findAll() {
        return repository.findAll().stream()
                .map(RegionEntityMapper::toDomain)
                .toList();
    }
}
