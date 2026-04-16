package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.model.Region;

public final class RegionEntityMapper {

    private RegionEntityMapper() {
    }

    public static RegionEntity toEntity(Region domain) {
        if (domain == null) {
            return null;
        }
        return new RegionEntity(
                domain.id(),
                domain.code(),
                domain.name(),
                domain.federalDistrictCode()
        );
    }

    public static Region toDomain(RegionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Region(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getFederalDistrictCode()
        );
    }
}
