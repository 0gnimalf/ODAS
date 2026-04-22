package Ogni.ODAS.application.dto.read;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.List;

public record IndicatorTreeNodeReadDto(
        Long id,
        Long indicatorId,
        String name,
        IndicatorGroupCode groupCode,
        Long parentIndicatorYearEntryId,
        Integer level,
        Integer sortOrder,
        boolean hasChildren,
        List<IndicatorTreeNodeReadDto> children
) {
}
