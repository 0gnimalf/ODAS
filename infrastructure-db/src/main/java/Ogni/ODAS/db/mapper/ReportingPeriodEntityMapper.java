package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.domain.model.ReportingPeriod;

public final class ReportingPeriodEntityMapper {

    private ReportingPeriodEntityMapper() {
    }

    public static ReportingPeriod toDomain(ReportingPeriodEntity entity) {
        return new ReportingPeriod(
                entity.getId(),
                entity.getPeriodType(),
                entity.getYear(),
                entity.getMonth(),
                entity.getQuarter(),
                entity.getDateFrom(),
                entity.getDateTo(),
                entity.getLabel()
        );
    }
}
