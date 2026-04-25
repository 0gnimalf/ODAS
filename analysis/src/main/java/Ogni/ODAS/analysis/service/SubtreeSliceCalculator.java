package Ogni.ODAS.analysis.service;

import Ogni.ODAS.application.dto.analysis.SubtreeSliceNodeDto;
import Ogni.ODAS.application.dto.analysis.SubtreeSliceResultDto;
import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.out.analysis.SubtreeSlicePort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SubtreeSliceCalculator implements SubtreeSlicePort {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public SubtreeSliceResultDto calculate(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            RegionReadDto region,
            IndicatorEntryReadDto rootEntry,
            ObservationValueKind valueKind,
            UnitCode unitCode,
            List<IndicatorEntryReadDto> subtreeEntries,
            List<ObservationReadDto> observations
    ) {
        Map<Long, ObservationReadDto> observationByEntryId = observations.stream()
                .collect(Collectors.toMap(ObservationReadDto::indicatorYearEntryId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, IndicatorEntryReadDto> byId = subtreeEntries.stream()
                .collect(Collectors.toMap(IndicatorEntryReadDto::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> pathByEntryId = buildPaths(subtreeEntries, byId);
        BigDecimal rootValue = Optional.ofNullable(observationByEntryId.get(rootEntry.id())).map(ObservationReadDto::value).orElse(null);

        List<SubtreeSliceNodeDto> nodes = subtreeEntries.stream()
                .sorted(Comparator.comparing(IndicatorEntryReadDto::level)
                        .thenComparing(IndicatorEntryReadDto::sortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(IndicatorEntryReadDto::name, Comparator.nullsLast(String::compareTo)))
                .map(entry -> toNode(entry, observationByEntryId, byId, pathByEntryId, rootValue))
                .toList();

        return new SubtreeSliceResultDto(
                groupCode,
                year,
                month,
                region.id(),
                region.name(),
                rootEntry.id(),
                rootEntry.name(),
                valueKind,
                valueKind.getLabel(),
                unitCode,
                unitCode.getLabel(),
                nodes
        );
    }

    private SubtreeSliceNodeDto toNode(
            IndicatorEntryReadDto entry,
            Map<Long, ObservationReadDto> observationByEntryId,
            Map<Long, IndicatorEntryReadDto> byId,
            Map<Long, String> pathByEntryId,
            BigDecimal rootValue
    ) {
        ObservationReadDto observation = observationByEntryId.get(entry.id());
        BigDecimal value = observation == null ? null : observation.value();
        BigDecimal parentValue = Optional.ofNullable(entry.parentIndicatorYearEntryId())
                .map(observationByEntryId::get)
                .map(ObservationReadDto::value)
                .orElse(null);
        return new SubtreeSliceNodeDto(
                entry.id(),
                entry.indicatorId(),
                entry.name(),
                entry.parentIndicatorYearEntryId(),
                entry.level(),
                entry.sortOrder(),
                entry.hasChildren(),
                pathByEntryId.get(entry.id()),
                value,
                observation == null,
                percent(value, parentValue),
                percent(value, rootValue)
        );
    }

    private BigDecimal percent(BigDecimal value, BigDecimal base) {
        if (value == null || base == null || base.signum() == 0) {
            return null;
        }
        return value.divide(base, MATH_CONTEXT).multiply(ONE_HUNDRED, MATH_CONTEXT);
    }

    private Map<Long, String> buildPaths(List<IndicatorEntryReadDto> entries, Map<Long, IndicatorEntryReadDto> byId) {
        Map<Long, String> cache = new LinkedHashMap<>();
        for (IndicatorEntryReadDto entry : entries) {
            buildPath(entry, byId, cache);
        }
        return cache;
    }

    private String buildPath(
            IndicatorEntryReadDto entry,
            Map<Long, IndicatorEntryReadDto> byId,
            Map<Long, String> cache
    ) {
        String cached = cache.get(entry.id());
        if (cached != null) {
            return cached;
        }
        if (entry.parentIndicatorYearEntryId() == null) {
            cache.put(entry.id(), entry.name());
            return entry.name();
        }
        IndicatorEntryReadDto parent = byId.get(entry.parentIndicatorYearEntryId());
        String value = parent == null ? entry.name() : buildPath(parent, byId, cache) + " / " + entry.name();
        cache.put(entry.id(), value);
        return value;
    }
}
