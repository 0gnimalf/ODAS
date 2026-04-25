package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record MonthlySeriesPointDto(
        Long periodId,
        Integer year,
        Integer month,
        String periodLabel,
        BigDecimal cumulativeValue,
        BigDecimal nonCumulativeValue,
        boolean nonCumulativeCalculated,
        boolean anomaly
) {
}
