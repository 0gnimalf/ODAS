package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.dto.CollectedReferenceCatalogDto;
import Ogni.ODAS.application.port.out.ExternalPopulationCollectorPort;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

@Configuration
@Profile("stub-collector")
public class CollectorStubConfig {

    @Bean
    public ExternalSourceCollectorPort externalSourceCollectorPort() {
        return new ExternalSourceCollectorPort() {
            @Override
            public CollectedDatasetDto collect(AnalyzeBudgetDataCommand command) {
                return new CollectedDatasetDto(
                        "stub-dataset",
                        "stub-version",
                        SourceSystemCode.OTHER,
                        List.<CollectedObservationDto>of()
                );
            }
        };
    }

    @Bean
    public ExternalPopulationCollectorPort externalPopulationCollectorPort() {
        return (regionCode, year) -> Optional.empty();
    }

    @Bean
    public ExternalReferenceCollectorPort externalReferenceCollectorPort() {
        return () -> new CollectedReferenceCatalogDto(List.of(), List.of());
    }
}
