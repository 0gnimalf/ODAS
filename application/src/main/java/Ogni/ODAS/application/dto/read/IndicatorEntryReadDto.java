package Ogni.ODAS.application.dto.read;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record IndicatorEntryReadDto(
        Long id,
        Long periodId,
        Long indicatorId,
        String name,
        IndicatorGroupCode groupCode,
        Long parentIndicatorYearEntryId,
        Integer level,
        Integer sortOrder,
        boolean hasChildren
) {
}
