package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface DatasetVersionJpaRepository extends JpaRepository<DatasetVersionEntity, Long> {

    Optional<DatasetVersionEntity> findBySourceSystemCodeAndExternalTitleAndExternalDateModified(
            SourceSystemCode sourceSystemCode,
            String externalTitle,
            OffsetDateTime externalDateModified
    );
}
