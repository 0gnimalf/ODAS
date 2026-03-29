package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaDatasetVersionRepository extends JpaRepository<DatasetVersionEntity, Long> {

    Optional<DatasetVersionEntity> findByDatasetCodeAndVersionLabelAndSourceSystem(
            String datasetCode,
            String versionLabel,
            SourceSystemCode sourceSystem
    );
}
