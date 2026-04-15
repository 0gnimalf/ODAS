package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.validation.DomainPreconditions;

public record IndicatorYearEntry(
        Long id,
        Long periodId,
        Long indicatorId,
        Long parentIndicatorYearEntryId,
        Integer level,
        Integer sortOrder,
        boolean hasChildren
) {
    public IndicatorYearEntry {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.validateId(periodId, "periodId");
        DomainPreconditions.validateId(indicatorId, "indicatorId");
        DomainPreconditions.validateId(parentIndicatorYearEntryId, "parentIndicatorYearEntryId");
        DomainPreconditions.nonNegative(level, "level");
        DomainPreconditions.nonNegative(sortOrder, "sortOrder");

        DomainPreconditions.require(
                (level == 0 && parentIndicatorYearEntryId == null) || (level > 0 && parentIndicatorYearEntryId != null),
                "root entry must not have parent and nested entry must have parent"
        );
    }

    public boolean isRoot() {
        return parentIndicatorYearEntryId == null;
    }
}
