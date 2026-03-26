package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;

import java.time.OffsetDateTime;

public record DatasetVersion(
        Long id,
        String datasetCode,
        String versionLabel,
        SourceSystemCode sourceSystem,
        OffsetDateTime collectedAt,
        boolean current
) {
}
