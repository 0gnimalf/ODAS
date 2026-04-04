package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record Indicator(
        Long id,
        String code,
        IndicatorGroupCode groupCode
) {
}
