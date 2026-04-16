package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.domain.model.DatasetVersion;

public final class DatasetVersionEntityMapper {

    private DatasetVersionEntityMapper() {
    }

    public static DatasetVersionEntity toEntity(DatasetVersion domain) {
        if (domain == null) {
            return null;
        }
        return new DatasetVersionEntity(
                domain.id(),
                domain.sourceSystemCode(),
                domain.externalTitle(),
                domain.externalDateModified());
    }

    public static DatasetVersion toDomain(DatasetVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DatasetVersion(
                entity.getId(),
                entity.getSourceSystemCode(),
                entity.getExternalTitle(),
                entity.getExternalDateModified());
    }
}
