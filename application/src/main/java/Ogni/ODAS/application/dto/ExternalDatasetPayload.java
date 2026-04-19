package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;

import java.time.OffsetDateTime;
import java.util.List;

public record ExternalDatasetPayload(
        SourceSystemCode sourceSystemCode,
        String externalTitle,
        OffsetDateTime externalDateModified,
        String request,
        String rawData,
        List<ExternalObservationRow> observations
) {
    public ExternalDatasetPayload {
        observations = observations == null ? List.of() : List.copyOf(observations);
    }
}
