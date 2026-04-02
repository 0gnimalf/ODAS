package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.model.Region;
import org.springframework.stereotype.Component;

@Component
public class RegionEntityMapper {

    public Region toDomain(RegionEntity entity) {
        return new Region(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getFederalDistrictCode()
        );
    }

    public RegionEntity toNewEntity(Region domain) {
        RegionEntity entity = new RegionEntity();
        copyToEntity(domain, entity);
        return entity;
    }

    public void copyToEntity(Region domain, RegionEntity entity) {
        entity.setCode(domain.code());
        entity.setName(domain.name());
        entity.setFederalDistrictCode(domain.federalDistrictCode());
    }
}
