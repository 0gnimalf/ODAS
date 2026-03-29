package Ogni.ODAS.iminfin.config;

public enum IminfinPassportPage {
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
        return trimTrailingSlash(properties.getBaseUrl())
                + ensureLeadingSlash(properties.getPassportRoot())
                + ensureLeadingSlash(relativePath);
    }

    private static String ensureLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
