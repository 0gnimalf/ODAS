package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.model.PopulationStat;

import java.util.Optional;

public interface ExternalPopulationCollectorPort {

    Optional<PopulationStat> collect(
            String regionCode,
            Integer year
    );
}
