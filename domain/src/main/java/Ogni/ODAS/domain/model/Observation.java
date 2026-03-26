package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.math.BigDecimal;

public record Observation(
        Long id,

        DatasetVersion datasetVersion,
        String regionCode,
        String indicatorCode,

        ReportingPeriod reportingPeriod,

        ObservationValueKind valueKind,
        BigDecimal value,
        boolean cumulative
) {
}
