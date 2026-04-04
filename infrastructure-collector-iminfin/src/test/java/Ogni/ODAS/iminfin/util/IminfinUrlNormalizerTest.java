package Ogni.ODAS.iminfin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinUrlNormalizerTest {

    @Test
    void ensureLeadingSlashAddsSlashWhenMissing() {
        assertEquals("/path", IminfinUrlNormalizer.ensureLeadingSlash("path"));
        assertEquals("/path", IminfinUrlNormalizer.ensureLeadingSlash("/path"));
    }

    @Test
    void trimTrailingSlashRemovesOnlyOneTrailingSlash() {
        assertEquals("https://site", IminfinUrlNormalizer.trimTrailingSlash("https://site/"));
        assertEquals("https://site", IminfinUrlNormalizer.trimTrailingSlash("https://site"));
    }
}

