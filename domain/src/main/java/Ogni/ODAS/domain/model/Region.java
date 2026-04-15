package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.validation.DomainPreconditions;

public record Region(
        Long id,
        String code,
        String name,
        FederalDistrictCode federalDistrictCode
) {
    public Region {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(code, "code"),
                32,
                "code");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(name, "name"),
                255,
                "name");
        DomainPreconditions.notNull(federalDistrictCode, "federalDistrictCode");
    }
}
