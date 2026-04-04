package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record IndicatorYearEntry(
        Long id,
        Long indicatorId,
        String indicatorCode,
        IndicatorGroupCode groupCode,
        Integer year,
        String name,
        Long parentIndicatorId,
        Integer level,
        Integer sortOrder,
        boolean section
) {
}
