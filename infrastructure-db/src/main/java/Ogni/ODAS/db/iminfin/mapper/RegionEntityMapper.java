package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.model.Region;

public final class RegionEntityMapper {

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
}
