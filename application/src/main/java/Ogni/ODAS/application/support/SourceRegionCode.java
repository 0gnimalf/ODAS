package Ogni.ODAS.application.support;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;

public final class SourceRegionCode {

    private SourceRegionCode() {
    }

    public static String compose(SourceSystemCode sourceSystemCode, String externalCode) {
        if (sourceSystemCode == null) {
            throw new IllegalArgumentException("sourceSystemCode must not be null");
        }
        if (externalCode == null || externalCode.isBlank()) {
            throw new IllegalArgumentException("externalCode must not be blank");
        }
        return sourceSystemCode.name() + ":" + externalCode.trim();
    }

    public static String externalPart(String composedCode, SourceSystemCode expectedSource) {
        if (composedCode == null || composedCode.isBlank()) {
            throw new IllegalArgumentException("composedCode must not be blank");
        }
        String prefix = expectedSource.name() + ":";
        if (!composedCode.startsWith(prefix)) {
            throw new IllegalArgumentException("region code does not belong to source " + expectedSource + ": " + composedCode);
        }
        return composedCode.substring(prefix.length());
    }
}
