package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.RegionComparisonItemDto;
import Ogni.ODAS.application.dto.analysis.RegionComparisonResultDto;
import Ogni.ODAS.application.dto.analysis.RegionComparisonSummaryDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.out.analysis.RegionComparisonPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;

public class RegionComparisonCalculator implements RegionComparisonPort {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public RegionComparisonResultDto calculate(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            Long indicatorYearEntryId,
            String indicatorName,
            ObservationValueKind valueKind,
            UnitCode unitCode,
            List<RegionReadDto> requestedRegions,
            List<ObservationReadDto> observations
    ) {
        Map<Long, ObservationReadDto> observationByRegionId = observations.stream()
                .collect(Collectors.toMap(ObservationReadDto::regionId, observation -> observation, (left, right) -> left, LinkedHashMap::new));
        List<BigDecimal> existingValues = observations.stream()
                .map(ObservationReadDto::value)
                .sorted()
                .toList();
        BigDecimal total = existingValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal min = existingValues.isEmpty() ? null : existingValues.getFirst();
        BigDecimal max = existingValues.isEmpty() ? null : existingValues.getLast();
        BigDecimal average = existingValues.isEmpty() ? null : total.divide(BigDecimal.valueOf(existingValues.size()), MATH_CONTEXT);
        BigDecimal median = median(existingValues);
        BigDecimal leader = max;

        List<RegionReadDto> sortedRequestedRegions = requestedRegions.stream()
                .sorted(Comparator.comparing(RegionReadDto::name))
                .toList();

        List<ObservationReadDto> rankedObservations = observations.stream()
                .sorted(Comparator.comparing(ObservationReadDto::value, Comparator.reverseOrder())
                        .thenComparing(ObservationReadDto::regionName, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<Long, Integer> rankByRegionId = new LinkedHashMap<>();
        int rank = 1;
        for (ObservationReadDto observation : rankedObservations) {
            rankByRegionId.putIfAbsent(observation.regionId(), rank++);
        }

        List<RegionComparisonItemDto> items = new ArrayList<>(sortedRequestedRegions.size());
        for (RegionReadDto region : sortedRequestedRegions) {
            ObservationReadDto observation = observationByRegionId.get(region.id());
            BigDecimal value = observation == null ? null : observation.value();
            items.add(new RegionComparisonItemDto(
                    region.id(),
                    region.name(),
                    value,
                    observation == null,
                    observation == null ? null : rankByRegionId.get(region.id()),
                    shareOfTotal(value, total),
                    difference(value, leader),
                    difference(value, average)
            ));
        }

        RegionComparisonSummaryDto summary = new RegionComparisonSummaryDto(
                requestedRegions.size(),
                observations.size(),
                min,
                max,
                average,
                median,
                existingValues.isEmpty() ? null : total
        );
        return new RegionComparisonResultDto(
                groupCode,
                year,
                month,
                indicatorYearEntryId,
                indicatorName,
                valueKind,
                valueKind.getLabel(),
                unitCode,
                unitCode.getLabel(),
                summary,
                List.copyOf(items)
        );
    }

    private BigDecimal shareOfTotal(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.signum() == 0) {
            return null;
        }
        return value.divide(total, MATH_CONTEXT).multiply(ONE_HUNDRED, MATH_CONTEXT);
    }

    private BigDecimal difference(BigDecimal value, BigDecimal base) {
        return value == null || base == null ? null : value.subtract(base);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }
        return values.get(middle - 1).add(values.get(middle)).divide(BigDecimal.valueOf(2), MATH_CONTEXT);
    }
}
