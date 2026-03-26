package Ogni.ODAS.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AnalysisResultDto(
        String regionCode,
        String indicatorCode,

        Integer year,
        Integer month,

        String valueKind,
        BigDecimal value,

        BigDecimal perCapitaValue,
        BigDecimal growthRate,

        String sourceSystem,
        OffsetDateTime collectedAt,
        boolean fromCache
) {
}
