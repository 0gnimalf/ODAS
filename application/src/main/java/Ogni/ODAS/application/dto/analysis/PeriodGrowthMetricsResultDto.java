package Ogni.ODAS.application.dto.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.math.BigDecimal;

public record PeriodGrowthMetricsResultDto(
        IndicatorGroupCode groupCode,
        Long regionId,
        String regionName,
        Long indicatorYearEntryId,
        String indicatorName,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        String unitCodeLabel,
        Integer targetYear,
        Integer targetMonth,
        MonthlySeriesPointDto targetMonthPoint,
        MonthlySeriesPointDto previousMonthPoint,
        MonthlySeriesPointDto sameMonthPreviousYearPoint,
        BigDecimal absoluteDeltaToPreviousMonth,
        BigDecimal rateToPreviousMonthPercent,
        BigDecimal absoluteDeltaToSameMonthPreviousYear,
        BigDecimal rateToSameMonthPreviousYearPercent,
        QuarterAggregateDto currentQuarter,
        QuarterAggregateDto previousQuarter,
        QuarterAggregateDto sameQuarterPreviousYear,
        BigDecimal absoluteDeltaToPreviousQuarter,
        BigDecimal rateToPreviousQuarterPercent,
        BigDecimal absoluteDeltaToSameQuarterPreviousYear,
        BigDecimal rateToSameQuarterPreviousYearPercent,
        boolean autoCollectedMissing
) {
}
