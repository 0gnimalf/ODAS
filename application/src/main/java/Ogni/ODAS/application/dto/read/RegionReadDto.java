package Ogni.ODAS.application.dto.read;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;

public record RegionReadDto(
        Long id,
        String name,
        FederalDistrictCode federalDistrictCode,
        String federalDistrictName,
        String federalDistrictFullName,
        String federalDistrictShortName
) {
}
