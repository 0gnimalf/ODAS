package Ogni.ODAS.iminfin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinTextNormalizerTest {

    @Test
    void normalizeRemovesExtraSpacesAndNormalizesYo() {
        assertEquals("елка", IminfinTextNormalizer.normalize("  Ёлка   "));
        assertEquals("г. москва", IminfinTextNormalizer.normalize("г. Москва"));
    }

    @Test
    void slugifyBuildsStableSlug() {
        assertEquals("налоговые-и-неналоговые-доходы", IminfinTextNormalizer.slugify("Налоговые и неналоговые доходы"));
    }
}
