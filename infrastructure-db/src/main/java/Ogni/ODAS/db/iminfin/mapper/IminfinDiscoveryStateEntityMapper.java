package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryState;
import Ogni.ODAS.db.iminfin.entity.IminfinDiscoveryStateEntity;

public final class IminfinDiscoveryStateEntityMapper {

    private IminfinDiscoveryStateEntityMapper() {
    }

    public static IminfinDiscoveryState toDomain(IminfinDiscoveryStateEntity entity) {
        return new IminfinDiscoveryState(
                entity.getId(),
                entity.getProfileKey(),
                entity.getStatus(),
                entity.getLastSuccessAt(),
                entity.getLastErrorAt(),
                entity.getLastErrorMessage()
        );
    }

    public static IminfinDiscoveryStateEntity toEntity(IminfinDiscoveryState domain) {
        IminfinDiscoveryStateEntity entity = new IminfinDiscoveryStateEntity();
        entity.setId(domain.id());
        entity.setProfileKey(domain.profileKey());
        entity.setStatus(domain.status());
        entity.setLastSuccessAt(domain.lastSuccessAt());
        entity.setLastErrorAt(domain.lastErrorAt());
        entity.setLastErrorMessage(domain.lastErrorMessage());
        return entity;
    }
}
