package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.DatasetCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetCollectionJpaRepository extends JpaRepository<DatasetCollectionEntity, Long> {

    List<DatasetCollectionEntity> findAllByDatasetVersionIdOrderByCollectedAtDesc(Long datasetVersionId);

    Optional<DatasetCollectionEntity> findFirstByDatasetVersionIdOrderByCollectedAtDesc(Long datasetVersionId);
}
