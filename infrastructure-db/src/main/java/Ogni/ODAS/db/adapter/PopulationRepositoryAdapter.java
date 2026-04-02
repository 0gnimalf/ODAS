package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.db.entity.PopulationStatEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.mapper.PopulationStatEntityMapper;
import Ogni.ODAS.db.repository.JpaPopulationStatRepository;
import Ogni.ODAS.db.support.PersistenceReferenceResolver;
import Ogni.ODAS.domain.model.PopulationStat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PopulationRepositoryAdapter implements PopulationRepositoryPort {

    private final JpaPopulationStatRepository repository;
    private final PersistenceReferenceResolver referenceResolver;
    private final PopulationStatEntityMapper mapper;

    public PopulationRepositoryAdapter(
            JpaPopulationStatRepository repository,
            PersistenceReferenceResolver referenceResolver,
            PopulationStatEntityMapper mapper
    ) {
        this.repository = repository;
        this.referenceResolver = referenceResolver;
        this.mapper = mapper;
    }

    @Override
    public Optional<PopulationStat> findByRegionAndYear(String regionCode, Integer year) {
        return repository.findByRegionCodeAndReportingPeriodYear(regionCode, year)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public PopulationStat save(PopulationStat populationStat) {
        RegionEntity region = referenceResolver.resolveRegion(populationStat.regionCode());
        ReportingPeriodEntity reportingPeriod = referenceResolver.resolveReportingPeriod(populationStat.reportingPeriod());

        PopulationStatEntity entity = repository.findByRegionCodeAndReportingPeriodYear(
                        populationStat.regionCode(),
                        populationStat.reportingPeriod().year()
                )
                .orElseGet(() -> mapper.toNewEntity(populationStat, region, reportingPeriod));

        return mapper.toDomain(repository.save(entity));
    }
}
