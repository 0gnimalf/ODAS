package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.model.DatasetCollection;

public interface DatasetCollectionPersistencePort {

    DatasetCollection save(DatasetCollection datasetCollection);
}
