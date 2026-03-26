package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.model.PopulationStat;

import java.util.Optional;

public interface PopulationRepositoryPort {

    Optional<PopulationStat> findByRegionAndPeriod(
            String regionCode,
            Integer year,
            Integer month
    );
}
