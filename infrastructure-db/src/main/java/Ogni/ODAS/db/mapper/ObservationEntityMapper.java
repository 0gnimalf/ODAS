package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.ObservationEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
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
                entity.getIndicator().getIndicatorGroupCode(),
                entity.getIndicator().getCode(),
                ReportingPeriodEntityMapper.toDomain(entity.getReportingPeriod()),
                entity.getValueKind(),
                entity.getValue(),
                entity.isCumulative()
        );
    }

    public ObservationEntity toEntity(Observation domain, DatasetVersionEntity datasetVersionEntity) {
        return new ObservationEntity(
                domain.id(),
                datasetVersionEntity,
                resolveRegion(domain.regionCode()),
                resolveIndicator(domain.indicatorCode(), domain.indicatorGroupCode()),
                resolveReportingPeriod(domain),
                domain.valueKind(),
                domain.value(),
                domain.cumulative()
        );
    }

    private RegionEntity resolveRegion(String regionCode) {
        return regionRepository.findByCode(regionCode)
                .orElseGet(() -> {
                    RegionEntity region = new RegionEntity();
                    region.setCode(regionCode);
                    region.setName("Заглушка|" + regionCode);
                    region.setFederalDistrictCode(FederalDistrictCode.NONE);
                    return regionRepository.save(region);
                });
    }

    private IndicatorEntity resolveIndicator(String indicatorCode, IndicatorGroupCode groupCode) {
        return indicatorRepository.findByCodeAndIndicatorGroupCode(indicatorCode, groupCode)
                .orElseGet(() -> {
                    IndicatorEntity indicator = new IndicatorEntity();
                    indicator.setCode(indicatorCode);
                    indicator.setName("Заглушка|" + indicatorCode);
                    indicator.setIndicatorGroupCode(groupCode == null ? IndicatorGroupCode.OTHER : groupCode);
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
                    period.setLabel(p.label());
                    return reportingPeriodRepository.save(period);
                });
    }
}
