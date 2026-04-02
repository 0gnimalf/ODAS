package Ogni.ODAS.iminfin.util;

public final class IminfinUrlNormalizer {

    private IminfinUrlNormalizer() {
    }

    public static String ensureLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    public static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
