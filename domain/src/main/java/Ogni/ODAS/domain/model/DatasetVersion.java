package Ogni.ODAS.domain.model;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.validation.DomainPreconditions;

import java.time.OffsetDateTime;

public record DatasetVersion(
        Long id,
        SourceSystemCode sourceSystemCode,
        String externalTitle,
        OffsetDateTime externalDateModified
) {
    public DatasetVersion {
        DomainPreconditions.validateId(id, "id");
        DomainPreconditions.notNull(sourceSystemCode, "sourceSystemCode");
        DomainPreconditions.maxLength(
                DomainPreconditions.notBlank(externalTitle, "externalTitle"),
                500,
                "externalTitle"
        );
        DomainPreconditions.notNull(externalDateModified, "externalDateModified");
    }
}
