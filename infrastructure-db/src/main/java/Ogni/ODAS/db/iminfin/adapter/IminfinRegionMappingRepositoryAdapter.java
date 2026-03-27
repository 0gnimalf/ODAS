package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinRegionMapping;
import Ogni.ODAS.application.iminfin.port.out.IminfinRegionMappingRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.IminfinRegionMappingEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinRegionMappingRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IminfinRegionMappingRepositoryAdapter implements IminfinRegionMappingRepositoryPort {

    private final JpaIminfinRegionMappingRepository repository;
    private final JpaRegionRepository regionRepository;

    public IminfinRegionMappingRepositoryAdapter(
            JpaIminfinRegionMappingRepository repository,
            JpaRegionRepository regionRepository
    ) {
        this.repository = repository;
        this.regionRepository = regionRepository;
    }

    @Override
    public Optional<IminfinRegionMapping> findByExternalTerritoryCode(String externalTerritoryCode) {
        return repository.findByExternalTerritoryCode(externalTerritoryCode)
                .map(IminfinRegionMappingEntityMapper::toDomain);
    }

    @Override
    public IminfinRegionMapping save(IminfinRegionMapping mapping) {
        var regionEntity = regionRepository.findByCode(mapping.regionCode())
                .orElseThrow(() -> new IllegalStateException("Region not found by code: " + mapping.regionCode()));

        var entity = repository.findByExternalTerritoryCode(mapping.externalTerritoryCode())
                .orElseGet(() -> IminfinRegionMappingEntityMapper.toEntity(mapping, regionEntity));

        entity.setSourceSystem(mapping.sourceSystem());
        entity.setRawTerritoryName(mapping.rawTerritoryName());
        entity.setRegion(regionEntity);

        return IminfinRegionMappingEntityMapper.toDomain(repository.save(entity));
    }
}
