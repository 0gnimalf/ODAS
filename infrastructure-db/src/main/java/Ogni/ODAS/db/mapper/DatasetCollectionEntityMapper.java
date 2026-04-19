package Ogni.ODAS.db.mapper;

import Ogni.ODAS.application.support.JsonSupport;
import Ogni.ODAS.db.entity.DatasetCollectionEntity;
import Ogni.ODAS.domain.model.DatasetCollection;

public final class DatasetCollectionEntityMapper {

    private DatasetCollectionEntityMapper() {
    }

    public static DatasetCollectionEntity toEntity(DatasetCollection domain) {
        if (domain == null) {
            return null;
        }
        return new DatasetCollectionEntity(
                domain.id(),
                domain.datasetVersionId(),
                domain.collectedAt(),
                domain.request(),
                JsonSupport.readTree(domain.rawData())
        );
    }

    public static DatasetCollection toDomain(DatasetCollectionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DatasetCollection(
                entity.getId(),
                entity.getDatasetVersionId(),
                entity.getCollectedAt(),
                entity.getRequest(),
                JsonSupport.write(entity.getRawData())
        );
    }
}
