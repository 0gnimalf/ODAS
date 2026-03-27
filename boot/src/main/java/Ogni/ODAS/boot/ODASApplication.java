package Ogni.ODAS.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "Ogni.ODAS")
@EnableJpaRepositories(basePackages = {
        "Ogni.ODAS.db.repository",
        "Ogni.ODAS.db.iminfin.repository"

})
@EntityScan(basePackages = {
        "Ogni.ODAS.db.entity",
        "Ogni.ODAS.db.iminfin.entity"
})
public class ODASApplication {

    public static void main(String[] args) {
        SpringApplication.run(ODASApplication.class, args);
    }
}
