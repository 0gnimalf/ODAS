package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.*;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.stereotype.Component;

@Component
public class ObservationEntityMapper {

    private final JpaRegionRepository regionRepository;
    private final JpaIndicatorRepository indicatorRepository;
    private final JpaReportingPeriodRepository reportingPeriodRepository;

    public ObservationEntityMapper(
            JpaRegionRepository regionRepository,
            JpaIndicatorRepository indicatorRepository,
            JpaReportingPeriodRepository reportingPeriodRepository
    ) {
        this.regionRepository = regionRepository;
        this.indicatorRepository = indicatorRepository;
        this.reportingPeriodRepository = reportingPeriodRepository;
    }

    public Observation toDomain(ObservationEntity entity) {
        return new Observation(
                entity.getId(),
                DatasetVersionEntityMapper.toDomain(entity.getDatasetVersion()),
                entity.getRegion().getCode(),
                entity.getIndicator().getCode(),
                ReportingPeriodEntityMapper.toDomain(entity.getReportingPeriod()),
                entity.getValueKind(),
                entity.getValue(),
                entity.isCumulative()
        );
    }

    public ObservationEntity toEntity(Observation domain, DatasetVersionEntity datasetVersionEntity) {
        ObservationEntity entity = new ObservationEntity();
        entity.setId(domain.id());
        entity.setDatasetVersion(datasetVersionEntity);
        entity.setRegion(resolveRegion(domain.regionCode()));
        entity.setIndicator(resolveIndicator(domain.indicatorCode()));
        entity.setReportingPeriod(resolveReportingPeriod(domain));
        entity.setValueKind(domain.valueKind());
        entity.setValue(domain.value());
        entity.setCumulative(domain.cumulative());
        return entity;
    }

    private RegionEntity resolveRegion(String regionCode) {
        return regionRepository.findByCode(regionCode)
                .orElseGet(() -> {
                    RegionEntity region = new RegionEntity();
                    region.setCode(regionCode);
                    region.setName(regionCode);
                    region.setFederalDistrictCode(FederalDistrictCode.NONE);
                    return regionRepository.save(region);
                });
    }

    private IndicatorEntity resolveIndicator(String indicatorCode) {
        return indicatorRepository.findByCode(indicatorCode)
                .orElseGet(() -> {
                    IndicatorEntity indicator = new IndicatorEntity();
                    indicator.setCode(indicatorCode);
                    indicator.setName(indicatorCode);
                    indicator.setIndicatorGroupCode(IndicatorGroupCode.OTHER);
                    indicator.setSection(false);
                    return indicatorRepository.save(indicator);
                });
    }

    private ReportingPeriodEntity resolveReportingPeriod(Observation domain) {
        var p = domain.reportingPeriod();

        return reportingPeriodRepository.findByPeriodTypeAndYearAndMonthAndQuarter(
                        p.type(),
                        p.year(),
                        p.month(),
                        p.quarter()
                )
                .orElseGet(() -> {
                    ReportingPeriodEntity period = new ReportingPeriodEntity();
                    period.setPeriodType(p.type());
                    period.setYear(p.year());
                    period.setMonth(p.month());
                    period.setQuarter(p.quarter());
                    period.setDateFrom(p.dateFrom());
                    period.setDateTo(p.dateTo());
                    period.setLabel(p.label());
                    return reportingPeriodRepository.save(period);
                });
    }
}
