package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.validation.DomainPreconditions;

import java.time.OffsetDateTime;

public record DatasetCollection(
        Long id,
        Long datasetVersionId,
        OffsetDateTime collectedAt,
        String request,
        String rawData
) {
    public DatasetCollection {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.validateId(datasetVersionId, "datasetVersionId");
        DomainPreconditions.notNull(collectedAt, "collectedAt");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(request, "request"),
                4000,
                "request");
        DomainPreconditions.notBlank(rawData, "rawData");
    }
}
