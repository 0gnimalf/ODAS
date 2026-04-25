package Ogni.ODAS.application.port.out.analysis;

import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.PeriodGrowthMetricsResultDto;
import Ogni.ODAS.application.dto.analysis.QuarterAggregateDto;

import java.math.BigDecimal;
import java.util.List;

public interface PeriodGrowthMetricsPort {

    MetricsSnapshot calculate(List<MonthlySeriesPointDto> points, List<QuarterAggregateDto> quarterAggregates, int targetYear, int targetMonth);

    record MetricsSnapshot(
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
            BigDecimal rateToSameQuarterPreviousYearPercent
    ) {
        public PeriodGrowthMetricsResultDto applyMetadata(
                Ogni.ODAS.domain.enumtype.IndicatorGroupCode groupCode,
                Long regionId,
                String regionName,
                Long indicatorYearEntryId,
                String indicatorName,
                Ogni.ODAS.domain.enumtype.ObservationValueKind valueKind,
                String valueKindLabel,
                Ogni.ODAS.domain.enumtype.UnitCode unitCode,
                String unitCodeLabel,
                Integer year,
                Integer month,
                boolean autoCollectedMissing
        ) {
            return new PeriodGrowthMetricsResultDto(
                    groupCode,
                    regionId,
                    regionName,
                    indicatorYearEntryId,
                    indicatorName,
                    valueKind,
                    valueKindLabel,
                    unitCode,
                    unitCodeLabel,
                    year,
                    month,
                    targetMonthPoint,
                    previousMonthPoint,
                    sameMonthPreviousYearPoint,
                    absoluteDeltaToPreviousMonth,
                    rateToPreviousMonthPercent,
                    absoluteDeltaToSameMonthPreviousYear,
                    rateToSameMonthPreviousYearPercent,
                    currentQuarter,
                    previousQuarter,
                    sameQuarterPreviousYear,
                    absoluteDeltaToPreviousQuarter,
                    rateToPreviousQuarterPercent,
                    absoluteDeltaToSameQuarterPreviousYear,
                    rateToSameQuarterPreviousYearPercent,
                    autoCollectedMissing
            );
        }
    }
}
