package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record MonthlyObservationPointRawDto(
        Long periodId,
        Integer year,
        Integer month,
        String periodLabel,
        BigDecimal value
) {
}
