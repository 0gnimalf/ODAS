package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.validation.DomainPreconditions;

public record Indicator(
        Long id,
        String name,
        IndicatorGroupCode indicatorGroupCode
) {
    public Indicator {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(name, "name"),
                2000,
                "name");
        DomainPreconditions.notNull(indicatorGroupCode, "indicatorGroupCode");
    }
}
