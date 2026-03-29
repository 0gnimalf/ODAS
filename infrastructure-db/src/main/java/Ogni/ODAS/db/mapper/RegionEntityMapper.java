package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.model.Region;

public class RegionEntityMapper {

    private RegionEntityMapper() {
    }

    public static Region toDomain(RegionEntity entity) {
        return new Region(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getFederalDistrictCode()
        );
    }

    public static RegionEntity toEntity(Region domain) {
        return new RegionEntity(
                domain.id(),
                domain.code(),
                domain.name(),
                domain.federalDistrictCode()
        );
    }
}
