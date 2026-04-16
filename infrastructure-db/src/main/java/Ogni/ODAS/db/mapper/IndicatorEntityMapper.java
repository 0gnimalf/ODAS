package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.domain.model.Indicator;

public final class IndicatorEntityMapper {

    private IndicatorEntityMapper() {
    }

    public static IndicatorEntity toEntity(Indicator domain) {
        if (domain == null) {
            return null;
        }
        return new IndicatorEntity(
                domain.id(),
                domain.name(),
                domain.indicatorGroupCode()
        );
    }

    public static Indicator toDomain(IndicatorEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Indicator(
                entity.getId(),
                entity.getName(),
                entity.getIndicatorGroupCode());
    }
}
