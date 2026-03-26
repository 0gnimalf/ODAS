package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.math.BigDecimal;

public record CollectedObservationDto(
        String regionCode,
        String indicatorCode,

        Integer year,
        Integer month,

        ObservationValueKind valueKind,
        BigDecimal value,

        boolean cumulative
) {
}
