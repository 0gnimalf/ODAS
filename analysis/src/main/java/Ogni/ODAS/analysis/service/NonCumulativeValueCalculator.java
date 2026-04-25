package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.MonthlyObservationPointRawDto;
import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.NonCumulativeValueMode;
import Ogni.ODAS.application.port.out.analysis.NonCumulativeValuePort;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class NonCumulativeValueCalculator implements NonCumulativeValuePort {

    @Override
    public List<MonthlySeriesPointDto> calculate(
            List<MonthlyObservationPointRawDto> rawPoints,
            boolean absoluteValue,
            NonCumulativeValueMode mode
    ) {
        List<MonthlyObservationPointRawDto> sorted = rawPoints.stream()
                .sorted(Comparator.comparing(MonthlyObservationPointRawDto::year).thenComparing(MonthlyObservationPointRawDto::month))
                .toList();

        MonthlyObservationPointRawDto previous = null;
        java.util.ArrayList<MonthlySeriesPointDto> result = new java.util.ArrayList<>(sorted.size());
        for (MonthlyObservationPointRawDto current : sorted) {
            BigDecimal nonCumulativeValue = null;
            boolean calculated = false;
            boolean anomaly = false;

            if (absoluteValue) {
                if (current.month() == 1) {
                    nonCumulativeValue = current.value();
                    calculated = true;
                } else if (previous != null && YearMonth.of(previous.year(), previous.month()).plusMonths(1).equals(YearMonth.of(current.year(), current.month()))) {
                    nonCumulativeValue = current.value().subtract(previous.value());
                    calculated = true;
                    anomaly = nonCumulativeValue.signum() < 0;
                }
            }

            result.add(new MonthlySeriesPointDto(
                    current.periodId(),
                    current.year(),
                    current.month(),
                    current.periodLabel(),
                    current.value(),
                    nonCumulativeValue,
                    calculated,
                    anomaly
            ));
            previous = current;
        }
        return List.copyOf(result);
    }
}
