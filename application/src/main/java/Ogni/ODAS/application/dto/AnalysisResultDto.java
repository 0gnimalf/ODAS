package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AnalysisResultDto(
        String regionCode,
        String indicatorCode,

        Integer year,
        Integer month,

        ObservationValueKind valueKind,
        BigDecimal value,

        BigDecimal perCapitaValue,
        BigDecimal growthRate,

        SourceSystemCode sourceSystem,
        OffsetDateTime collectedAt,
        boolean fromCache
) {
}
