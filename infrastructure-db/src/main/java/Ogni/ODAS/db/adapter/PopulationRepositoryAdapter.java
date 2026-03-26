package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.db.mapper.PopulationStatEntityMapper;
import Ogni.ODAS.db.repository.JpaPopulationStatRepository;
import Ogni.ODAS.domain.model.PopulationStat;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PopulationRepositoryAdapter implements PopulationRepositoryPort {

    private final JpaPopulationStatRepository repository;

    public PopulationRepositoryAdapter(JpaPopulationStatRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PopulationStat> findByRegionAndPeriod(String regionCode, Integer year, Integer month) {
        return repository.findByRegion_CodeAndReportingPeriod_YearAndReportingPeriod_Month(
                        regionCode, year, month
                )
                .map(PopulationStatEntityMapper::toDomain);
    }
}
