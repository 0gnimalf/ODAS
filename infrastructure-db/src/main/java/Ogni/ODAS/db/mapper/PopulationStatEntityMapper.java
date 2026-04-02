package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.PopulationStatEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.domain.model.PopulationStat;
import org.springframework.stereotype.Component;

@Component
public class PopulationStatEntityMapper {

    private final ReportingPeriodEntityMapper reportingPeriodMapper;

    public PopulationStatEntityMapper(ReportingPeriodEntityMapper reportingPeriodMapper) {
        this.reportingPeriodMapper = reportingPeriodMapper;
    }

    public PopulationStat toDomain(PopulationStatEntity entity) {
        return new PopulationStat(
                entity.getId(),
                entity.getRegion().getCode(),
                reportingPeriodMapper.toDomain(entity.getReportingPeriod()),
                entity.getPopulationValue()
        );
    }

    public PopulationStatEntity toNewEntity(
            PopulationStat domain,
            RegionEntity region,
            ReportingPeriodEntity reportingPeriod
    ) {
        PopulationStatEntity entity = new PopulationStatEntity();
        copyToEntity(domain, region, reportingPeriod, entity);
        return entity;
    }

    public void copyToEntity(
            PopulationStat domain,
            RegionEntity region,
            ReportingPeriodEntity reportingPeriod,
            PopulationStatEntity entity
    ) {
        entity.setRegion(region);
        entity.setReportingPeriod(reportingPeriod);
        entity.setPopulationValue(domain.populationValue());
    }
}
