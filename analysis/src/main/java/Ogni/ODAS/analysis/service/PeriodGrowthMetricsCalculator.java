package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.QuarterAggregateDto;
import Ogni.ODAS.application.port.out.analysis.PeriodGrowthMetricsPort;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PeriodGrowthMetricsCalculator implements PeriodGrowthMetricsPort {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public MetricsSnapshot calculate(List<MonthlySeriesPointDto> points, List<QuarterAggregateDto> quarterAggregates, int targetYear, int targetMonth) {
        boolean useNonCumulativeValues = points.stream().anyMatch(MonthlySeriesPointDto::nonCumulativeCalculated);
        Map<String, MonthlySeriesPointDto> pointsByKey = points.stream()
                .collect(Collectors.toMap(this::monthKey, Function.identity(), (left, right) -> left));
        MonthlySeriesPointDto target = pointsByKey.get(monthKey(targetYear, targetMonth));
        MonthlySeriesPointDto previousMonth = pointsByKey.get(monthKey(targetMonth == 1 ? targetYear - 1 : targetYear, targetMonth == 1 ? 12 : targetMonth - 1));
        MonthlySeriesPointDto sameMonthPreviousYear = pointsByKey.get(monthKey(targetYear - 1, targetMonth));

        int currentQuarterNumber = quarter(targetMonth);
        QuarterAggregateDto currentQuarter = quarterAggregates.stream()
                .filter(item -> item.year() == targetYear && item.quarter() == currentQuarterNumber)
                .findFirst()
                .orElse(null);
        QuarterAggregateDto previousQuarter = quarterAggregates.stream()
                .filter(item -> item.year() == previousQuarterYear(targetYear, currentQuarterNumber)
                        && item.quarter() == previousQuarterNumber(currentQuarterNumber))
                .findFirst()
                .orElse(null);
        QuarterAggregateDto sameQuarterPreviousYear = quarterAggregates.stream()
                .filter(item -> item.year() == targetYear - 1 && item.quarter() == currentQuarterNumber)
                .findFirst()
                .orElse(null);

        BigDecimal targetValue = effectiveValue(target, useNonCumulativeValues);
        BigDecimal previousMonthValue = effectiveValue(previousMonth, useNonCumulativeValues);
        BigDecimal sameMonthPreviousYearValue = effectiveValue(sameMonthPreviousYear, useNonCumulativeValues);
        BigDecimal currentQuarterValue = effectiveQuarterValue(currentQuarter);
        BigDecimal previousQuarterValue = effectiveQuarterValue(previousQuarter);
        BigDecimal sameQuarterPreviousYearValue = effectiveQuarterValue(sameQuarterPreviousYear);

        return new MetricsSnapshot(
                target,
                previousMonth,
                sameMonthPreviousYear,
                subtractOrNull(targetValue, previousMonthValue),
                percentRate(targetValue, previousMonthValue),
                subtractOrNull(targetValue, sameMonthPreviousYearValue),
                percentRate(targetValue, sameMonthPreviousYearValue),
                currentQuarter,
                previousQuarter,
                sameQuarterPreviousYear,
                subtractOrNull(currentQuarterValue, previousQuarterValue),
                percentRate(currentQuarterValue, previousQuarterValue),
                subtractOrNull(currentQuarterValue, sameQuarterPreviousYearValue),
                percentRate(currentQuarterValue, sameQuarterPreviousYearValue)
        );
    }

    private BigDecimal effectiveValue(MonthlySeriesPointDto point, boolean useNonCumulativeValues) {
        if (point == null) {
            return null;
        }
        if (!useNonCumulativeValues) {
            return point.cumulativeValue();
        }
        return point.nonCumulativeCalculated() ? point.nonCumulativeValue() : null;
    }

    private BigDecimal effectiveQuarterValue(QuarterAggregateDto quarter) {
        if (quarter == null || !quarter.complete()) {
            return null;
        }
        return quarter.aggregatedValue();
    }

    private BigDecimal subtractOrNull(BigDecimal current, BigDecimal base) {
        return current == null || base == null ? null : current.subtract(base);
    }

    private BigDecimal percentRate(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.signum() == 0) {
            return null;
        }
        return current.divide(base, MATH_CONTEXT)
                .subtract(BigDecimal.ONE)
                .multiply(ONE_HUNDRED, MATH_CONTEXT);
    }

    private String monthKey(MonthlySeriesPointDto point) {
        return monthKey(point.year(), point.month());
    }

    private String monthKey(int year, int month) {
        return year + "-" + month;
    }

    private int quarter(int month) {
        return ((month - 1) / 3) + 1;
    }

    private int previousQuarterYear(int year, int quarter) {
        return quarter == 1 ? year - 1 : year;
    }

    private int previousQuarterNumber(int quarter) {
        return quarter == 1 ? 4 : quarter - 1;
    }
}
