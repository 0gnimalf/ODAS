package Ogni.ODAS.iminfin.config;

import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.*;

public enum IminfinPassportPage {
    PASSPORT_ROOT(""),
    INCOMES_COMPARE("dokhody-sravnenie-po-regionam"),
    INCOMES_DETAIL("dokhody-detalno"),
    OUTCOMES_COMPARE("raskhody-sravnenie-po-regionam"),
    OUTCOMES_DETAIL("raskhody-detalno"),
    CREDITS_COMPARE("kredity-sravnenie-po-regionam"),
    FIN_SOURCES_DETAIL("istochniki-finansirovaniya-detalno");

    private final String relativePath;

    IminfinPassportPage(String relativePath) {
        this.relativePath = relativePath;
    }

    public String relativePath() {
        return relativePath;
    }

    public String pageUrl(IminfinCollectorProperties properties) {
        String root = trimTrailingSlash(properties.getBaseUrl())
                + ensureLeadingSlash(properties.getPassportRoot());
        if (relativePath == null || relativePath.isBlank()) {
            return root;
        }
        return root + ensureLeadingSlash(relativePath);
    }
}
