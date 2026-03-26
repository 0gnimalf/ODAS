package Ogni.ODAS.domain.model;

import java.math.BigDecimal;

public record Observation(
        Long id,

        DatasetVersion datasetVersion,
        String regionCode,
        String indicatorCode,

        ReportingPeriod reportingPeriod,

        String valueKind,
        BigDecimal value,
        boolean cumulative
) {
}
