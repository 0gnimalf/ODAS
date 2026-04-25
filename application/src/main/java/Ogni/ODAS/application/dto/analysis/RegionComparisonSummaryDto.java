package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record RegionComparisonSummaryDto(
        int requestedRegionCount,
        int foundRegionCount,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal averageValue,
        BigDecimal medianValue,
        BigDecimal totalValue
) {
}
