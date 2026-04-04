package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.model.Region;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionEntityMapperTest {

    private final RegionEntityMapper mapper = new RegionEntityMapper();

    @Test
    void mapsDomainToEntityAndBack() {
        Region domain = new Region(1L, "77", "Москва", FederalDistrictCode.CFO);

        RegionEntity entity = mapper.toNewEntity(domain);
        entity.setId(1L);

        assertEquals("77", entity.getCode());
        assertEquals("Москва", mapper.toDomain(entity).name());
    }
}

