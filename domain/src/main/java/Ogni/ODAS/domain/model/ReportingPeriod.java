package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.PeriodType;

import java.time.LocalDate;

public record ReportingPeriod(
        Long id,
        PeriodType type,
        Integer year,
        Integer month,
        Integer quarter,
        LocalDate dateFrom,
        LocalDate dateTo,
        String label
) {
}
