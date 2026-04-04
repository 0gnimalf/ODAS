package Ogni.ODAS.iminfin.util;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class IminfinPeriodFormatter {

    private static final DateTimeFormatter IMINFIN_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private IminfinPeriodFormatter() {
    }

    public static String format(int year, int month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .format(IMINFIN_PERIOD_FORMAT);
    }
}
