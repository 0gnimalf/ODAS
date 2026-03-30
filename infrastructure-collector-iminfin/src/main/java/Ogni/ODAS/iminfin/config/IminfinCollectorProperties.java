package Ogni.ODAS.iminfin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iminfin")
public class IminfinCollectorProperties {

    private String baseUrl = "https://www.iminfin.ru";
    private String passportRoot = "/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf";
    private Integer minYearToCollect = 2015;
    private Integer maxYearToCollect = 2025;
    private Duration connectTimeout = Duration.ofSeconds(15);
    private Duration readTimeout = Duration.ofSeconds(90);
    private Duration discoveryTtl = Duration.ofHours(6);

}
