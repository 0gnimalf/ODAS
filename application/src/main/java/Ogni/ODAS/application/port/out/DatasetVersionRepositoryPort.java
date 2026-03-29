package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;

import java.util.Optional;

public interface DatasetVersionRepositoryPort {

    DatasetVersion save(DatasetVersion datasetVersion);

    Optional<DatasetVersion> findByDatasetCodeAndVersionLabelAndSourceSystem(
            String datasetCode,
            String versionLabel,
            SourceSystemCode sourceSystem
    );
}
