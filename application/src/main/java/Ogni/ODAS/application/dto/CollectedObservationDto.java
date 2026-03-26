package Ogni.ODAS.application.dto;

import java.math.BigDecimal;

public record CollectedObservationDto(
        String regionCode,
        String indicatorCode,

        Integer year,
        Integer month,

        String valueKind,
        BigDecimal value,

        boolean cumulative
) {
}
