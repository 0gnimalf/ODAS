package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.model.IminfinReportSession;
import Ogni.ODAS.application.iminfin.port.out.IminfinReportSessionRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.IminfinReportSessionEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinReportSessionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IminfinReportSessionRepositoryAdapter implements IminfinReportSessionRepositoryPort {

    private final JpaIminfinReportSessionRepository repository;

    public IminfinReportSessionRepositoryAdapter(JpaIminfinReportSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IminfinReportSession> findLatestByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findFirstByProfileKeyOrderByResolvedAtDesc(profileKey)
                .map(IminfinReportSessionEntityMapper::toDomain);
    }

    @Override
    public IminfinReportSession save(IminfinReportSession session) {
        var saved = repository.save(IminfinReportSessionEntityMapper.toEntity(session));
        return IminfinReportSessionEntityMapper.toDomain(saved);
    }
}
