package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.PeriodType;

public record ReportingPeriod(
        Long id,
        PeriodType type,
        Integer year,
        Integer month,
        Integer quarter,
        String label
) {
}
