package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record CollectObservationsCommand(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        List<Long> regionIds
) {
    public CollectObservationsCommand(
            IndicatorGroupCode groupCode,
            Integer year,
            Integer month
    ) {
        this(groupCode, year, month, List.of());
    }

    public CollectObservationsCommand {
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(month, "month must not be null");
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        regionIds = normalizeRegionIds(regionIds);
    }

    private static List<Long> normalizeRegionIds(Collection<Long> regionCodes) {
        if (regionCodes == null || regionCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long code : regionCodes) {
            if (code == null) {
                continue;
            }
            normalized.add(code);
        }
        return List.copyOf(normalized);
    }

    public boolean allRegionsRequested() {
        return regionIds.isEmpty();
    }
}
