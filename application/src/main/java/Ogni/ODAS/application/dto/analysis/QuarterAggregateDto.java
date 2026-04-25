package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record QuarterAggregateDto(
        Integer year,
        Integer quarter,
        String label,
        BigDecimal aggregatedValue,
        int coveredMonthCount,
        boolean complete
) {
}
