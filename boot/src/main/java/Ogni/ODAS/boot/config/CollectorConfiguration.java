package Ogni.ODAS.boot.config;

import Ogni.ODAS.iminfin.collector.IminfinCollector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OdasIminfinProperties.class)
public class CollectorConfiguration {

    @Bean
    public IminfinCollector iminfinCollector(OdasIminfinProperties properties) {
        return new IminfinCollector(properties.toCollectorProperties());
    }
}
