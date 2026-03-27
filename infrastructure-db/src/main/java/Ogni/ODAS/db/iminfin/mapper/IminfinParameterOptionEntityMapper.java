package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.application.iminfin.model.IminfinParameterOption;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterOptionEntity;

public final class IminfinParameterOptionEntityMapper {

    private IminfinParameterOptionEntityMapper() {
    }

    public static IminfinParameterOption toDomain(IminfinParameterOptionEntity entity) {
        return new IminfinParameterOption(
                entity.getId(),
                entity.getProfileKey(),
                entity.getParameterName(),
                entity.getExternalValue(),
                entity.getRawLabel(),
                entity.getInternalOptionCode(),
                entity.getVerificationStatus(),
                entity.getDiscoveredAt()
        );
    }

    public static IminfinParameterOptionEntity toEntity(IminfinParameterOption domain) {
        IminfinParameterOptionEntity entity = new IminfinParameterOptionEntity();
        entity.setId(domain.id());
        entity.setProfileKey(domain.profileKey());
        entity.setParameterName(domain.parameterName());
        entity.setExternalValue(domain.externalValue());
        entity.setRawLabel(domain.rawLabel());
        entity.setInternalOptionCode(domain.internalOptionCode());
        entity.setVerificationStatus(domain.verificationStatus());
        entity.setDiscoveredAt(domain.discoveredAt());
        return entity;
    }
}
