package Ogni.ODAS.iminfin.util;

import java.text.Normalizer;
import java.util.Locale;

public final class IminfinTextNormalizer {

    private IminfinTextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace(' ', ' ')
                .replace('ё', 'е')
                .replace('Ё', 'Е')
                .trim()
                .replaceAll("\\s+", " ");

        return Normalizer.normalize(normalized, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    public static String slugify(String value) {
        return normalize(value)
                .replaceAll("[^a-zа-я0-9]+", "-")
                .replaceAll("(^-+|-+$)", "")
                .replaceAll("-+", "-");
    }
}
