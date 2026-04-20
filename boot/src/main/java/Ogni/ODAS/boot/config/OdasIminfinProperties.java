package Ogni.ODAS.boot.config;

import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "odas.iminfin")
public class OdasIminfinProperties {

    private String baseUrl;
    private String passportRoot;
    private Duration connectTimeout;
    private Duration readTimeout;
    private Duration discoveryTtl;
    private Integer maxParallelRequests;
    private Integer retryAttempts;
    private Duration retryBackoff;

    public IminfinCollectorProperties toCollectorProperties() {
        return new IminfinCollectorProperties(
                baseUrl,
                passportRoot,
                connectTimeout,
                readTimeout,
                discoveryTtl,
                maxParallelRequests,
                retryAttempts,
                retryBackoff
        );
    }
}
