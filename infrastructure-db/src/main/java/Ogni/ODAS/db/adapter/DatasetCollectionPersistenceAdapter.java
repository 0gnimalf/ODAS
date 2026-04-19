package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.DatasetCollectionPersistencePort;
import Ogni.ODAS.db.mapper.DatasetCollectionEntityMapper;
import Ogni.ODAS.db.repository.DatasetCollectionJpaRepository;
import Ogni.ODAS.domain.model.DatasetCollection;

public class DatasetCollectionPersistenceAdapter implements DatasetCollectionPersistencePort {

    private final DatasetCollectionJpaRepository repository;

    public DatasetCollectionPersistenceAdapter(DatasetCollectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DatasetCollection save(DatasetCollection datasetCollection) {
        return DatasetCollectionEntityMapper.toDomain(repository.save(DatasetCollectionEntityMapper.toEntity(datasetCollection)));
    }
}
