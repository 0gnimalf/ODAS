package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record CollectedIndicatorDto(
        String code,
        String name,
        IndicatorGroupCode groupCode,
        String parentCode,
        Integer level,
        Integer sortOrder,
        boolean section
) {
}
