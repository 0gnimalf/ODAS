package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.QuarterAggregateDto;
import Ogni.ODAS.application.port.out.analysis.QuarterAggregationPort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuarterAggregationCalculator implements QuarterAggregationPort {

    @Override
    public List<QuarterAggregateDto> aggregate(List<MonthlySeriesPointDto> points) {
        Map<QuarterKey, List<MonthlySeriesPointDto>> grouped = new LinkedHashMap<>();
        for (MonthlySeriesPointDto point : points) {
            grouped.computeIfAbsent(new QuarterKey(point.year(), quarter(point.month())), ignored -> new ArrayList<>())
                    .add(point);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toQuarterAggregate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private QuarterAggregateDto toQuarterAggregate(QuarterKey key, List<MonthlySeriesPointDto> points) {
        BigDecimal aggregated = BigDecimal.ZERO;
        int coveredMonthCount = 0;
        for (MonthlySeriesPointDto point : points) {
            if (point.nonCumulativeCalculated() && point.nonCumulativeValue() != null) {
                aggregated = aggregated.add(point.nonCumulativeValue());
                coveredMonthCount++;
            }
        }
        return new QuarterAggregateDto(
                key.year,
                key.quarter,
                "Q" + key.quarter + " " + key.year,
                coveredMonthCount == 0 ? null : aggregated,
                coveredMonthCount,
                coveredMonthCount == 3
        );
    }

    private int quarter(int month) {
        return ((month - 1) / 3) + 1;
    }

    private static final class QuarterKey implements Comparable<QuarterKey> {
        private final int year;
        private final int quarter;

        private QuarterKey(int year, int quarter) {
            this.year = year;
            this.quarter = quarter;
        }

        @Override
        public int compareTo(QuarterKey other) {
            int yearCompare = Integer.compare(this.year, other.year);
            if (yearCompare != 0) {
                return yearCompare;
            }
            return Integer.compare(this.quarter, other.quarter);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof QuarterKey key)) {
                return false;
            }
            return year == key.year && quarter == key.quarter;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(year, quarter);
        }
    }
}
