package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
import Ogni.ODAS.application.port.in.ReferenceCatalogUseCase;
import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.application.port.out.ObservationRepositoryPort;
import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.application.service.AnalyzeBudgetDataService;
import Ogni.ODAS.application.service.ReferenceCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Bean
    public ReferenceCatalogUseCase referenceCatalogUseCase(
            ExternalReferenceCollectorPort externalReferenceCollectorPort,
            RegionRepositoryPort regionRepositoryPort,
            IndicatorRepositoryPort indicatorRepositoryPort
    ) {
        return new ReferenceCatalogService(
                externalReferenceCollectorPort,
                regionRepositoryPort,
                indicatorRepositoryPort
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
