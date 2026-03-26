package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.PopulationStatEntity;
import Ogni.ODAS.domain.model.PopulationStat;

public final class PopulationStatEntityMapper {

    private PopulationStatEntityMapper() {
    }

    public static PopulationStat toDomain(PopulationStatEntity entity) {
        return new PopulationStat(
                entity.getId(),
                entity.getRegion().getCode(),
                ReportingPeriodEntityMapper.toDomain(entity.getReportingPeriod()),
                entity.getPopulationValue()
        );
    }
}