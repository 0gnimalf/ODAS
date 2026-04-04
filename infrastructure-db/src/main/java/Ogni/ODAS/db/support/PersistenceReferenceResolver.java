package Ogni.ODAS.db.support;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.mapper.ReportingPeriodEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.springframework.stereotype.Component;

@Component
public class PersistenceReferenceResolver {

    private static final String PLACEHOLDER_PREFIX = "Заглушка|";

    private final JpaRegionRepository regionRepository;
    private final JpaIndicatorRepository indicatorRepository;
    private final JpaReportingPeriodRepository reportingPeriodRepository;
    private final ReportingPeriodEntityMapper reportingPeriodMapper;
    private final JpaDatasetVersionRepository datasetVersionRepository;
    private final DatasetVersionEntityMapper datasetVersionMapper;

    public PersistenceReferenceResolver(
            JpaRegionRepository regionRepository,
            JpaIndicatorRepository indicatorRepository,
            JpaReportingPeriodRepository reportingPeriodRepository,
            ReportingPeriodEntityMapper reportingPeriodMapper,
            JpaDatasetVersionRepository datasetVersionRepository,
            DatasetVersionEntityMapper datasetVersionMapper
    ) {
        this.regionRepository = regionRepository;
        this.indicatorRepository = indicatorRepository;
        this.reportingPeriodRepository = reportingPeriodRepository;
        this.reportingPeriodMapper = reportingPeriodMapper;
        this.datasetVersionRepository = datasetVersionRepository;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    public RegionEntity resolveRegion(String regionCode) {
        return regionRepository.findByCode(regionCode)
                .orElseGet(() -> {
                    RegionEntity region = new RegionEntity();
                    region.setCode(regionCode);
                    region.setName(PLACEHOLDER_PREFIX + regionCode);
                    region.setFederalDistrictCode(FederalDistrictCode.NONE);
                    return regionRepository.save(region);
                });
    }

    public IndicatorEntity resolveIndicator(String indicatorCode, IndicatorGroupCode groupCode) {
        IndicatorGroupCode safeGroupCode = groupCode == null ? IndicatorGroupCode.OTHER : groupCode;
        return indicatorRepository.findByCodeAndIndicatorGroupCode(indicatorCode, safeGroupCode)
                .orElseGet(() -> {
                    IndicatorEntity indicator = new IndicatorEntity();
                    indicator.setCode(indicatorCode);
                    indicator.setIndicatorGroupCode(safeGroupCode);
                    return indicatorRepository.save(indicator);
                });
    }

    public ReportingPeriodEntity resolveReportingPeriod(ReportingPeriod reportingPeriod) {
        return reportingPeriodRepository.findByPeriodTypeAndYearAndMonthAndQuarter(
                        reportingPeriod.type(),
                        reportingPeriod.year(),
                        reportingPeriod.month(),
                        reportingPeriod.quarter()
                )
                .orElseGet(() -> reportingPeriodRepository.save(reportingPeriodMapper.toNewEntity(reportingPeriod)));
    }

    public DatasetVersionEntity resolveDatasetVersion(DatasetVersion datasetVersion) {
        DatasetVersionEntity entity = datasetVersion.id() == null
                ? datasetVersionRepository.findByDatasetCodeAndVersionLabelAndSourceSystem(
                datasetVersion.datasetCode(),
                datasetVersion.versionLabel(),
                datasetVersion.sourceSystem()
        ).orElseGet(DatasetVersionEntity::new)
                : datasetVersionRepository.findById(datasetVersion.id()).orElseGet(DatasetVersionEntity::new);

        datasetVersionMapper.copyToEntity(datasetVersion, entity);
        return datasetVersionRepository.save(entity);
    }
}
