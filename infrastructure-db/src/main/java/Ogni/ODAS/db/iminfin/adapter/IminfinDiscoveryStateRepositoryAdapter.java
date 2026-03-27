package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryState;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.port.out.IminfinDiscoveryStateRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.IminfinDiscoveryStateEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinDiscoveryStateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IminfinDiscoveryStateRepositoryAdapter implements IminfinDiscoveryStateRepositoryPort {

    private final JpaIminfinDiscoveryStateRepository repository;

    public IminfinDiscoveryStateRepositoryAdapter(JpaIminfinDiscoveryStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IminfinDiscoveryState> findByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findByProfileKey(profileKey)
                .map(IminfinDiscoveryStateEntityMapper::toDomain);
    }

    @Override
    public IminfinDiscoveryState save(IminfinDiscoveryState state) {
        var entity = repository.findByProfileKey(state.profileKey())
                .orElseGet(() -> IminfinDiscoveryStateEntityMapper.toEntity(state));

        entity.setStatus(state.status());
        entity.setLastSuccessAt(state.lastSuccessAt());
        entity.setLastErrorAt(state.lastErrorAt());
        entity.setLastErrorMessage(state.lastErrorMessage());

        return IminfinDiscoveryStateEntityMapper.toDomain(repository.save(entity));
    }
}
