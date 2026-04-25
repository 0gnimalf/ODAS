package Ogni.ODAS.application.command.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.Objects;

import static Ogni.ODAS.domain.validation.DomainPreconditions.inRange;

public record CalculatePeriodGrowthMetricsCommand(
        IndicatorGroupCode groupCode,
        Long regionId,
        Long indicatorYearEntryId,
        ObservationValueKind valueKind,
        Integer year,
        Integer month,
        boolean autoCollectMissing
) {
    public CalculatePeriodGrowthMetricsCommand {
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(regionId, "regionId must not be null");
        Objects.requireNonNull(indicatorYearEntryId, "indicatorYearEntryId must not be null");
        Objects.requireNonNull(valueKind, "valueKind must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(month, "month must not be null");
        inRange(month, 1, 12, "month");
    }
}
