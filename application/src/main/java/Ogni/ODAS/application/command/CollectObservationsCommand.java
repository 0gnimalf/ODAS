package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.List;
import java.util.Objects;

import static Ogni.ODAS.domain.validation.DomainPreconditions.inRange;
import static Ogni.ODAS.domain.validation.DomainPreconditions.normalizeOptionalIds;

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
        inRange(month, 1, 12, "month");
        regionIds = normalizeOptionalIds(regionIds);
    }

    public boolean allRegionsRequested() {
        return regionIds.isEmpty();
    }
}
