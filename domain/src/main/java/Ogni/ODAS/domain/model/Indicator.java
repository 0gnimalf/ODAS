package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record Indicator(
        Long id,
        String code,
        String name,
        IndicatorGroupCode groupCode,
        Long parentId,
        Integer level,
        Integer sortOrder,
        boolean section,
        boolean core,
        boolean active
) {
}
