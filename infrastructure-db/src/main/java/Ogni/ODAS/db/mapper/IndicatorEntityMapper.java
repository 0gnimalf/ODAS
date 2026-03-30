package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.domain.model.Indicator;

public final class IndicatorEntityMapper {

    private IndicatorEntityMapper() {
    }

    public static Indicator toDomain(IndicatorEntity entity) {
        return new Indicator(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getIndicatorGroupCode(),
                entity.getParent() == null ? null : entity.getParent().getId(),
                entity.getLevel(),
                entity.getSortOrder(),
                entity.isSection()
        );
    }
}
