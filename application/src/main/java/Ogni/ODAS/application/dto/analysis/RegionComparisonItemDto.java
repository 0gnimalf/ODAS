package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record RegionComparisonItemDto(
        Long regionId,
        String regionName,
        BigDecimal value,
        boolean missing,
        Integer rank,
        BigDecimal shareOfTotalPercent,
        BigDecimal deltaFromLeader,
        BigDecimal deltaFromAverage
) {
}
