package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

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

            @Override
            public List<Integer> getDesiredObservationIndexes() {
                return List.of();
            }
        };
    }
}
