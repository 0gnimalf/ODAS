package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.application.iminfin.model.IminfinRegionMapping;
import Ogni.ODAS.db.iminfin.entity.IminfinRegionMappingEntity;
import Ogni.ODAS.db.entity.RegionEntity;

public final class IminfinRegionMappingEntityMapper {

    private IminfinRegionMappingEntityMapper() {
    }

    public static IminfinRegionMapping toDomain(IminfinRegionMappingEntity entity) {
        return new IminfinRegionMapping(
                entity.getId(),
                entity.getSourceSystem(),
                entity.getExternalTerritoryCode(),
                entity.getRawTerritoryName(),
                entity.getRegion().getCode()
        );
    }

    public static IminfinRegionMappingEntity toEntity(IminfinRegionMapping domain, RegionEntity regionEntity) {
        IminfinRegionMappingEntity entity = new IminfinRegionMappingEntity();
        entity.setId(domain.id());
        entity.setSourceSystem(domain.sourceSystem());
        entity.setExternalTerritoryCode(domain.externalTerritoryCode());
        entity.setRawTerritoryName(domain.rawTerritoryName());
        entity.setRegion(regionEntity);
        return entity;
    }
}
