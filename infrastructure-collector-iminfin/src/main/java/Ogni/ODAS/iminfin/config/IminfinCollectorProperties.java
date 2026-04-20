package Ogni.ODAS.iminfin.config;

import java.time.Duration;

import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.ensureLeadingSlash;
import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.trimTrailingSlash;

public record IminfinCollectorProperties(
        String baseUrl,
        String passportRoot,
        Duration connectTimeout,
        Duration readTimeout,
        Duration discoveryTtl,
        Integer maxParallelRequests,
        Integer retryAttempts,
        Duration retryBackoff
) {
    public IminfinCollectorProperties {
        baseUrl = isBlank(baseUrl) ? "https://www.iminfin.ru" : trimTrailingSlash(baseUrl);
        passportRoot = isBlank(passportRoot) ? "/areas-of-analysis/budget/finansoviy-pasport-subjecta-rf" : ensureLeadingSlash(passportRoot);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(90) : readTimeout;
        discoveryTtl = discoveryTtl == null ? Duration.ofHours(6) : discoveryTtl;
        maxParallelRequests = maxParallelRequests == null ? 4 : Math.clamp(maxParallelRequests, 1, 8);
        retryAttempts = retryAttempts == null ? 3 : Math.max(1, retryAttempts);
        retryBackoff = retryBackoff == null ? Duration.ofSeconds(2) : retryBackoff;
    }

    public static IminfinCollectorProperties defaults() {
        return new IminfinCollectorProperties(null, null, null, null, null, null, null, null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
