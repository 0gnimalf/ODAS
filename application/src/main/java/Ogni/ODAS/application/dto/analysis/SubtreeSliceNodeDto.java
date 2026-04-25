package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record SubtreeSliceNodeDto(
        Long indicatorYearEntryId,
        Long indicatorId,
        String indicatorName,
        Long parentIndicatorYearEntryId,
        Integer level,
        Integer sortOrder,
        boolean hasChildren,
        String path,
        BigDecimal value,
        boolean missing,
        BigDecimal shareOfParentPercent,
        BigDecimal shareOfRootPercent
) {
}
