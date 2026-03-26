package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CollectorStubConfig {

    @Bean
    public ExternalSourceCollectorPort externalSourceCollectorPort() {
        return new ExternalSourceCollectorPort() {
            @Override
            public List<CollectedObservationDto> collect(AnalyzeBudgetDataCommand command) {
                return List.of();
            }
        };
    }
}
