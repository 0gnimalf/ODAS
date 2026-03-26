package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;

public record Region (
        Long id,
        String code,
        String name,
        FederalDistrictCode federalDistrictCode
){
}
