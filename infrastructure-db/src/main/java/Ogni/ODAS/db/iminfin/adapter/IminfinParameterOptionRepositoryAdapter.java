package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinParameterOption;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterOptionRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.IminfinParameterOptionEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinParameterOptionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IminfinParameterOptionRepositoryAdapter implements IminfinParameterOptionRepositoryPort {

    private final JpaIminfinParameterOptionRepository repository;

    public IminfinParameterOptionRepositoryAdapter(JpaIminfinParameterOptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IminfinParameterOption> findByProfileKeyAndParameterName(
            IminfinReportProfileKey profileKey,
            String parameterName
    ) {
        return repository.findAllByProfileKeyAndParameterNameOrderByExternalValue(profileKey, parameterName)
                .stream()
                .map(IminfinParameterOptionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public IminfinParameterOption save(IminfinParameterOption option) {
        var entity = repository.findByProfileKeyAndParameterNameAndExternalValue(
                        option.profileKey(),
                        option.parameterName(),
                        option.externalValue()
                )
                .orElseGet(() -> IminfinParameterOptionEntityMapper.toEntity(option));

        entity.setRawLabel(option.rawLabel());
        entity.setInternalOptionCode(option.internalOptionCode());
        entity.setVerificationStatus(option.verificationStatus());
        entity.setDiscoveredAt(option.discoveredAt());

        return IminfinParameterOptionEntityMapper.toDomain(repository.save(entity));
    }
}
