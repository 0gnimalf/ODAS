package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.db.support.PersistenceReferenceResolver;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DatasetVersionRepositoryAdapter implements DatasetVersionRepositoryPort {

    private final JpaDatasetVersionRepository repository;
    private final DatasetVersionEntityMapper mapper;
    private final PersistenceReferenceResolver persistenceResolver;

    public DatasetVersionRepositoryAdapter(
            JpaDatasetVersionRepository repository,
            DatasetVersionEntityMapper mapper,
            PersistenceReferenceResolver persistenceResolver
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.persistenceResolver = persistenceResolver;
    }

    @Override
    @Transactional
    public DatasetVersion save(DatasetVersion datasetVersion) {
        DatasetVersionEntity datasetVersionEntity = persistenceResolver.resolveDatasetVersion(datasetVersion);
        return mapper.toDomain(datasetVersionEntity);
    }

    @Override
    public Optional<DatasetVersion> findByDatasetCodeAndVersionLabelAndSourceSystem(
            String datasetCode,
            String versionLabel,
            SourceSystemCode sourceSystem
    ) {
        return repository.findByDatasetCodeAndVersionLabelAndSourceSystem(datasetCode, versionLabel, sourceSystem)
                .map(mapper::toDomain);
    }
}
