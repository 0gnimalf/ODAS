package Ogni.ODAS.db.iminfin.mapper;

import Ogni.ODAS.application.iminfin.model.IminfinReportSession;
import Ogni.ODAS.db.iminfin.entity.IminfinReportSessionEntity;

public final class IminfinReportSessionEntityMapper {

    private IminfinReportSessionEntityMapper() {
    }

    public static IminfinReportSession toDomain(IminfinReportSessionEntity entity) {
        return new IminfinReportSession(
                entity.getId(),
                entity.getProfileKey(),
                entity.getReportId(),
                entity.getUuid(),
                entity.getVersionLabel(),
                entity.getDataVersion(),
                entity.getResolvedAt(),
                entity.getStatus()
        );
    }

    public static IminfinReportSessionEntity toEntity(IminfinReportSession domain) {
        IminfinReportSessionEntity entity = new IminfinReportSessionEntity();
        entity.setId(domain.id());
        entity.setProfileKey(domain.profileKey());
        entity.setReportId(domain.reportId());
        entity.setUuid(domain.uuid());
        entity.setVersionLabel(domain.versionLabel());
        entity.setDataVersion(domain.dataVersion());
        entity.setResolvedAt(domain.resolvedAt());
        entity.setStatus(domain.status());
        return entity;
    }
}
