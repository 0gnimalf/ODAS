package Ogni.ODAS.boot.temp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping("/internal/temp")
public class TemporaryHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "module", "boot",
                "temporaryApi", true,
                "time", OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }
}
