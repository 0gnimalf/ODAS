package Ogni.ODAS.application.command.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.List;
import java.util.Objects;

import static Ogni.ODAS.domain.validation.DomainPreconditions.inRange;
import static Ogni.ODAS.domain.validation.DomainPreconditions.normalizeNonEmptyIds;

public record CompareRegionsCommand(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long indicatorYearEntryId,
        ObservationValueKind valueKind,
        List<Long> regionIds,
        boolean forceRefresh
) {
    public CompareRegionsCommand {
        Objects.requireNonNull(groupCode, "groupCode must not be null");
        Objects.requireNonNull(year, "year must not be null");
        Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(indicatorYearEntryId, "indicatorYearEntryId must not be null");
        Objects.requireNonNull(valueKind, "valueKind must not be null");
        inRange(month, 1, 12, "month");
        regionIds = normalizeNonEmptyIds(regionIds, "regionIds");
    }
}
