package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.model.DatasetVersion;

public interface DatasetVersionRepositoryPort {

    DatasetVersion save(DatasetVersion datasetVersion);
}
