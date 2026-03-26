package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.domain.model.DatasetVersion;

public final class DatasetVersionEntityMapper {

    private DatasetVersionEntityMapper() {
    }

    public static DatasetVersion toDomain(DatasetVersionEntity entity) {
        return new DatasetVersion(
                entity.getId(),
                entity.getDatasetCode(),
                entity.getVersionLabel(),
                entity.getSourceSystem(),
                entity.getCollectedAt(),
                entity.isCurrent()
        );
    }

    public static DatasetVersionEntity toEntity(DatasetVersion domain) {
        return new DatasetVersionEntity(
                domain.id(),
                domain.datasetCode(),
                domain.versionLabel(),
                domain.sourceSystem(),
                domain.collectedAt(),
                domain.current());
    }
}
