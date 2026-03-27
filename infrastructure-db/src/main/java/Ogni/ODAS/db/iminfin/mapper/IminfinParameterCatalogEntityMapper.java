package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterCatalogEntity;

import java.time.OffsetDateTime;

public final class IminfinParameterCatalogEntityMapper {

    private IminfinParameterCatalogEntityMapper() {
    }

    public static IminfinParameterCatalogItem toDomain(IminfinParameterCatalogEntity entity) {
        return new IminfinParameterCatalogItem(
                entity.getId(),
                entity.getProfileKey(),
                entity.getParameterName(),
                entity.getRole(),
                entity.getValueType(),
                entity.isRequired(),
                entity.getDefaultValue()
        );
    }

    public static IminfinParameterCatalogEntity toEntity(IminfinParameterCatalogItem domain) {
        IminfinParameterCatalogEntity entity = new IminfinParameterCatalogEntity();
        entity.setId(domain.id());
        entity.setProfileKey(domain.profileKey());
        entity.setParameterName(domain.parameterName());
        entity.setRole(domain.role());
        entity.setValueType(domain.valueType());
        entity.setRequired(domain.required());
        entity.setDefaultValue(domain.defaultValue());
        entity.setLastSeenAt(OffsetDateTime.now());
        return entity;
    }
}
