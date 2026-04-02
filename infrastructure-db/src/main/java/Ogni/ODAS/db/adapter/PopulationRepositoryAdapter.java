package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.db.entity.PopulationStatEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.mapper.PopulationStatEntityMapper;
import Ogni.ODAS.db.repository.JpaPopulationStatRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.model.PopulationStat;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PopulationRepositoryAdapter implements PopulationRepositoryPort {

    private final JpaPopulationStatRepository repository;
    private final JpaRegionRepository regionRepository;
    private final JpaReportingPeriodRepository reportingPeriodRepository;

    public PopulationRepositoryAdapter(
            JpaPopulationStatRepository repository,
            JpaRegionRepository regionRepository,
            JpaReportingPeriodRepository reportingPeriodRepository
    ) {
        this.repository = repository;
        this.regionRepository = regionRepository;
        this.reportingPeriodRepository = reportingPeriodRepository;
    }

    @Override
    public Optional<PopulationStat> findByRegionAndYear(String regionCode, Integer year) {
        return repository.findByRegionCodeAndReportingPeriodYear(
                        regionCode, year
                )
                .map(PopulationStatEntityMapper::toDomain);
    }

    @Override
    public PopulationStat save(PopulationStat populationStat) {
        PopulationStatEntity entity = repository.findByRegionCodeAndReportingPeriodYear(
                        populationStat.regionCode(),
                        populationStat.reportingPeriod().year()
                )
                .orElseGet(PopulationStatEntity::new);

        entity.setRegion(resolveRegion(populationStat.regionCode()));
        entity.setReportingPeriod(resolveReportingPeriod(populationStat));
        entity.setPopulationValue(populationStat.populationValue());

        return PopulationStatEntityMapper.toDomain(repository.save(entity));
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

    private ReportingPeriodEntity resolveReportingPeriod(PopulationStat populationStat) {
        var period = populationStat.reportingPeriod();
        return reportingPeriodRepository.findByPeriodTypeAndYearAndMonthAndQuarter(
                        period.type(),
                        period.year(),
                        period.month(),
                        period.quarter()
                )
                .orElseGet(() -> {
                    ReportingPeriodEntity entity = new ReportingPeriodEntity();
                    entity.setPeriodType(period.type());
                    entity.setYear(period.year());
                    entity.setMonth(period.month());
                    entity.setQuarter(period.quarter());
                    entity.setLabel(period.label());
                    return reportingPeriodRepository.save(entity);
                });
    }
}
