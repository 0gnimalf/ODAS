package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.domain.model.DatasetVersion;
import org.springframework.stereotype.Component;

@Component
public class DatasetVersionRepositoryAdapter implements DatasetVersionRepositoryPort {

    private final JpaDatasetVersionRepository repository;

    public DatasetVersionRepositoryAdapter(JpaDatasetVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public DatasetVersion save(DatasetVersion datasetVersion) {
        var saved = repository.save(DatasetVersionEntityMapper.toEntity(datasetVersion));
        return DatasetVersionEntityMapper.toDomain(saved);
    }
}
