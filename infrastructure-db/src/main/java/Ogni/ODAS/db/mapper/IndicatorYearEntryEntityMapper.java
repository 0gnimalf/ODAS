package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.IndicatorYearEntryEntity;
import Ogni.ODAS.domain.model.IndicatorYearEntry;

public final class IndicatorYearEntryEntityMapper {

    private IndicatorYearEntryEntityMapper() {
    }

    public static IndicatorYearEntryEntity toEntity(IndicatorYearEntry domain) {
        if (domain == null) {
            return null;
        }
        return new IndicatorYearEntryEntity(
                domain.id(),
                domain.periodId(),
                domain.indicatorId(),
                domain.parentIndicatorYearEntryId(),
                domain.level(),
                domain.sortOrder(),
                domain.hasChildren()
        );
    }

    public static IndicatorYearEntry toDomain(IndicatorYearEntryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new IndicatorYearEntry(
                entity.getId(),
                entity.getPeriodId(),
                entity.getIndicatorId(),
                entity.getParentIndicatorYearEntryId(),
                entity.getLevel(),
                entity.getSortOrder(),
                entity.isHasChildren()
        );
    }
}
