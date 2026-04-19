package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.DatasetVersionPersistencePort;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.repository.DatasetVersionJpaRepository;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;

import java.time.OffsetDateTime;
import java.util.Optional;

public class DatasetVersionPersistenceAdapter implements DatasetVersionPersistencePort {

    private final DatasetVersionJpaRepository repository;

    public DatasetVersionPersistenceAdapter(DatasetVersionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DatasetVersion save(DatasetVersion datasetVersion) {
        return DatasetVersionEntityMapper.toDomain(repository.save(DatasetVersionEntityMapper.toEntity(datasetVersion)));
    }

    @Override
    public Optional<DatasetVersion> findByIdentity(SourceSystemCode sourceSystemCode, String externalTitle, OffsetDateTime externalDateModified) {
        return repository.findBySourceSystemCodeAndExternalTitleAndExternalDateModified(
                        sourceSystemCode,
                        externalTitle,
                        externalDateModified
                )
                .map(DatasetVersionEntityMapper::toDomain);
    }
}
