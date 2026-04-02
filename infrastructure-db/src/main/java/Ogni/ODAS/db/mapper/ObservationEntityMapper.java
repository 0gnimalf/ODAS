package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.*;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.stereotype.Component;

@Component
public class ObservationEntityMapper {

    private final DatasetVersionEntityMapper datasetVersionMapper;
    private final ReportingPeriodEntityMapper reportingPeriodMapper;

    public ObservationEntityMapper(
            DatasetVersionEntityMapper datasetVersionMapper,
            ReportingPeriodEntityMapper reportingPeriodMapper
    ) {
        this.datasetVersionMapper = datasetVersionMapper;
        this.reportingPeriodMapper = reportingPeriodMapper;
    }

    public Observation toDomain(ObservationEntity entity) {
        return new Observation(
                entity.getId(),
                datasetVersionMapper.toDomain(entity.getDatasetVersion()),
                entity.getRegion().getCode(),
                entity.getIndicator().getIndicatorGroupCode(),
                entity.getIndicator().getCode(),
                reportingPeriodMapper.toDomain(entity.getReportingPeriod()),
                entity.getValueKind(),
                entity.getValue(),
                entity.isCumulative()
        );
    }

    public ObservationEntity toNewEntity(
            Observation domain,
            DatasetVersionEntity datasetVersion,
            RegionEntity region,
            IndicatorEntity indicator,
            ReportingPeriodEntity reportingPeriod
    ) {
        ObservationEntity entity = new ObservationEntity();
        copyToEntity(domain, datasetVersion, region, indicator, reportingPeriod, entity);
        return entity;
    }

    public void copyToEntity(
            Observation domain,
            DatasetVersionEntity datasetVersion,
            RegionEntity region,
            IndicatorEntity indicator,
            ReportingPeriodEntity reportingPeriod,
            ObservationEntity entity
    ) {
        entity.setDatasetVersion(datasetVersion);
        entity.setRegion(region);
        entity.setIndicator(indicator);
        entity.setReportingPeriod(reportingPeriod);
        entity.setValueKind(domain.valueKind());
        entity.setValue(domain.value());
        entity.setCumulative(domain.cumulative());
    }
}
