package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;
import Ogni.ODAS.application.iminfin.port.out.IminfinReportSessionRepositoryPort;
import Ogni.ODAS.db.iminfin.entity.IminfinReportSessionEntity;
import Ogni.ODAS.db.iminfin.mapper.IminfinReportSessionEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinReportSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class IminfinReportSessionRepositoryAdapter implements IminfinReportSessionRepositoryPort {

    private final JpaIminfinReportSessionRepository repository;

    public IminfinReportSessionRepositoryAdapter(JpaIminfinReportSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public IminfinReportSession save(IminfinReportSession session) {
        IminfinReportSessionEntity entity = repository.findByProfileKeyAndDataVersion(
                        session.profileKey(),
                        session.dataVersion()
                )
                .orElseGet(IminfinReportSessionEntity::new);

        entity.setProfileKey(session.profileKey());
        entity.setReportId(session.reportId());
        entity.setUuid(session.uuid());
        entity.setVersionLabel(session.versionLabel());
        entity.setDataVersion(session.dataVersion());
        entity.setResolvedAt(session.resolvedAt());
        entity.setStatus(session.status());

        IminfinReportSessionEntity saved = repository.save(entity);
        return IminfinReportSessionEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IminfinReportSession> findLatestByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findFirstByProfileKeyOrderByResolvedAtDesc(profileKey)
                .map(IminfinReportSessionEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IminfinReportSession> findByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findAllByProfileKeyOrderByResolvedAtDesc(profileKey)
                .stream()
                .map(IminfinReportSessionEntityMapper::toDomain)
                .toList();
    }
}