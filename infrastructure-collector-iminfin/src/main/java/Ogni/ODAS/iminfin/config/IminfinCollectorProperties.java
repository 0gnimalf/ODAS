package Ogni.ODAS.iminfin.config;

import java.time.Duration;

import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.ensureLeadingSlash;
import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.trimTrailingSlash;

public record IminfinCollectorProperties(
        String baseUrl,
        String passportRoot,
        Duration connectTimeout,
        Duration readTimeout,
        Duration discoveryTtl
) {
    public IminfinCollectorProperties {
        baseUrl = isBlank(baseUrl) ? "https://www.iminfin.ru" : trimTrailingSlash(baseUrl);
        passportRoot = isBlank(passportRoot) ? "/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf" : ensureLeadingSlash(passportRoot);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(90) : readTimeout;
        discoveryTtl = discoveryTtl == null ? Duration.ofHours(6) : discoveryTtl;
    }

    public static IminfinCollectorProperties defaults() {
        return new IminfinCollectorProperties(null, null, null, null, null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
