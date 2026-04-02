package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.domain.model.DatasetVersion;
import org.springframework.stereotype.Component;

@Component
public class DatasetVersionEntityMapper {

    public DatasetVersion toDomain(DatasetVersionEntity entity) {
        return new DatasetVersion(
                entity.getId(),
                entity.getDatasetCode(),
                entity.getVersionLabel(),
                entity.getSourceSystem(),
                entity.getCollectedAt(),
                entity.isCurrent()
        );
    }

    public DatasetVersionEntity toNewEntity(DatasetVersion domain) {
        DatasetVersionEntity entity = new DatasetVersionEntity();
        copyToEntity(domain, entity);
        return entity;
    }

    public void copyToEntity(DatasetVersion domain, DatasetVersionEntity entity) {
        entity.setDatasetCode(domain.datasetCode());
        entity.setVersionLabel(domain.versionLabel());
        entity.setSourceSystem(domain.sourceSystem());
        entity.setCollectedAt(domain.collectedAt());
        entity.setCurrent(domain.current());
    }
}
