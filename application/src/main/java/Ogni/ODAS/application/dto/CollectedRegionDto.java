package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;

public record CollectedRegionDto(
        String code,
        String name,
        FederalDistrictCode federalDistrictCode
) {
}
