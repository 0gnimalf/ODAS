package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface DatasetVersionPersistencePort {

    DatasetVersion save(DatasetVersion datasetVersion);

    Optional<DatasetVersion> findByIdentity(
            SourceSystemCode sourceSystemCode,
            String externalTitle,
            OffsetDateTime externalDateModified
    );
}
