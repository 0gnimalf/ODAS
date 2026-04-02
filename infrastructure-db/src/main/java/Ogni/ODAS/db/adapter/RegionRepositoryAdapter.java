package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.mapper.RegionEntityMapper;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.domain.model.Region;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RegionRepositoryAdapter implements RegionRepositoryPort {

    private final JpaRegionRepository repository;
    private final RegionEntityMapper mapper;

    public RegionRepositoryAdapter(JpaRegionRepository repository, RegionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Region save(Region region) {
        RegionEntity entity = repository.findByCode(region.code())
                .orElseGet(() -> region.id() == null
                        ? mapper.toNewEntity(region)
                        : repository.findById(region.id()).orElseGet(() -> mapper.toNewEntity(region)));

        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return repository.findByCode(code)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public List<Region> saveAll(List<Region> regions) {
        if (regions.isEmpty()) {
            return List.of();
        }

        Map<String, RegionEntity> entitiesByCode = repository.findAllByCodeIn(
                        regions.stream().map(Region::code).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(
                        RegionEntity::getCode,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        for (Region region : regions) {
            RegionEntity entity = entitiesByCode.computeIfAbsent(
                    region.code(),
                    code -> mapper.toNewEntity(region)
            );
            mapper.copyToEntity(region, entity);
        }

        Map<String, Region> savedByCode = repository.saveAll(List.copyOf(entitiesByCode.values())).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(Region::code, Function.identity()));

        return regions.stream()
                .map(region -> savedByCode.get(region.code()))
                .toList();
    }

    @Override
    public List<Region> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
