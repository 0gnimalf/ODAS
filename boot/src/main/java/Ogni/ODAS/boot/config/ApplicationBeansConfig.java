package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.application.port.out.ObservationRepositoryPort;
import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.application.service.AnalyzeBudgetDataService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeansConfig {

    @Bean
    public AnalyzeBudgetDataUseCase analyzeBudgetDataUseCase(
            ObservationRepositoryPort observationRepositoryPort,
            DatasetVersionRepositoryPort datasetVersionRepositoryPort,
            PopulationRepositoryPort populationRepositoryPort,
            ExternalSourceCollectorPort externalSourceCollectorPort
    ) {
        return new AnalyzeBudgetDataService(
                observationRepositoryPort,
                datasetVersionRepositoryPort,
                populationRepositoryPort,
                externalSourceCollectorPort
        );
    }
}
