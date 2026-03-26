package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDatasetVersionRepository extends JpaRepository<DatasetVersionEntity, Long> {
}
