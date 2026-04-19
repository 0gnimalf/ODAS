package Ogni.ODAS.iminfin.config;

import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.ensureLeadingSlash;
import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.trimTrailingSlash;

public enum IminfinPassportPage {
    PASSPORT_ROOT(""),
    INCOMES_DETAIL("dokhody-detalno"),
    OUTCOMES_DETAIL("raskhody-detalno"),
    CREDITS_COMPARE("kredity-sravnenie-po-regionam"),
    FIN_SOURCES_DETAIL("istochniki-finansirovaniya-detalno");

    private final String relativePath;

    IminfinPassportPage(String relativePath) {
        this.relativePath = relativePath;
    }

    public String pageUrl(IminfinCollectorProperties properties) {
        String root = trimTrailingSlash(properties.baseUrl())
                + ensureLeadingSlash(properties.passportRoot());
        if (relativePath.isBlank()) {
            return root;
        }
        return root + ensureLeadingSlash(relativePath);
    }
}
