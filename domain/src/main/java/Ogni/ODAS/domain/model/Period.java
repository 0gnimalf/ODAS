package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.validation.DomainPreconditions;

public record Period(
        Long id,
        PeriodType periodType,
        Integer year,
        Integer month,
        Integer quarter,
        String label
) {
    public Period {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.notNull(periodType, "periodType");
        DomainPreconditions.inRange(year, 2000, 2050, "year");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(label, "label"),
                100,
                "label");

        switch (periodType) {
            case YEAR -> {
                DomainPreconditions.require(month == null, "month must be null for YEAR period");
                DomainPreconditions.require(quarter == null, "quarter must be null for YEAR period");
            }
            case MONTH -> {
                DomainPreconditions.inRange(month, 1, 12, "month");
                DomainPreconditions.require(quarter == null, "quarter must be null for MONTH period");
            }
            case QUARTER -> {
                DomainPreconditions.inRange(quarter, 1, 4, "quarter");
                DomainPreconditions.require(month == null, "month must be null for QUARTER period");
            }
        }
    }

    public static Period year(Integer year) {
        return new Period(null, PeriodType.YEAR, year, null, null, "01.01." + year);
    }

    public static Period year(Integer year, String label) {
        return new Period(null, PeriodType.YEAR, year, null, null, label);
    }

    public static Period month(Integer year, Integer month) {
        return new Period(null, PeriodType.MONTH, year, month, null, "%02d.%d".formatted(month, year));
    }

    public static Period month(Integer year, Integer month, String label) {
        return new Period(null, PeriodType.MONTH, year, month, null, label);
    }

    public static Period quarter(Integer year, Integer quarter) {
        return new Period(null, PeriodType.QUARTER, year, null, quarter, "%d квартал %d".formatted(quarter, year));
    }

    public static Period quarter(Integer year, Integer quarter, String label) {
        return new Period(null, PeriodType.QUARTER, year, null, quarter, label);
    }

    public boolean isYear() {
        return periodType == PeriodType.YEAR;
    }
}
