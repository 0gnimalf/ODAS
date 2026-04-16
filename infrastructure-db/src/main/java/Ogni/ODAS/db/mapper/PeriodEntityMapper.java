package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.PeriodEntity;
import Ogni.ODAS.domain.model.Period;

public final class PeriodEntityMapper {

    private PeriodEntityMapper() {
    }

    public static PeriodEntity toEntity(Period domain) {
        if (domain == null) {
            return null;
        }
        return new PeriodEntity(
                domain.id(),
                domain.periodType(),
                domain.year(),
                domain.month(),
                domain.quarter(),
                domain.label()
        );
    }

    public static Period toDomain(PeriodEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Period(
                entity.getId(),
                entity.getPeriodType(),
                entity.getYear(),
                entity.getMonth(),
                entity.getQuarter(),
                entity.getLabel()
        );
    }
}
