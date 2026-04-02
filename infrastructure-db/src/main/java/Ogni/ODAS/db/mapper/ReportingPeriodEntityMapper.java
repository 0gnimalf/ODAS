package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.springframework.stereotype.Component;

@Component
public class ReportingPeriodEntityMapper {

    public ReportingPeriod toDomain(ReportingPeriodEntity entity) {
        return new ReportingPeriod(
                entity.getId(),
                entity.getPeriodType(),
                entity.getYear(),
                entity.getMonth(),
                entity.getQuarter(),
                entity.getLabel()
        );
    }

    public ReportingPeriodEntity toNewEntity(ReportingPeriod domain) {
        ReportingPeriodEntity entity = new ReportingPeriodEntity();
        copyToEntity(domain, entity);
        return entity;
    }

    public void copyToEntity(ReportingPeriod domain, ReportingPeriodEntity entity) {
        entity.setPeriodType(domain.type());
        entity.setYear(domain.year());
        entity.setMonth(domain.month());
        entity.setQuarter(domain.quarter());
        entity.setLabel(domain.label());
    }
}
