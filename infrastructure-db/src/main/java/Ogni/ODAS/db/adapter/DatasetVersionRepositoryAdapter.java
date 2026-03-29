package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    @Override
    public Optional<DatasetVersion> findByDatasetCodeAndVersionLabelAndSourceSystem(
            String datasetCode,
            String versionLabel,
            SourceSystemCode sourceSystem
    ) {
        return repository.findByDatasetCodeAndVersionLabelAndSourceSystem(datasetCode, versionLabel, sourceSystem)
                .map(DatasetVersionEntityMapper::toDomain);
    }
}
