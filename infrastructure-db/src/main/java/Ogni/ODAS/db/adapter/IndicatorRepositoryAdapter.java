package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.mapper.IndicatorEntityMapper;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class IndicatorRepositoryAdapter implements IndicatorRepositoryPort {

    private final JpaIndicatorRepository repository;
    private final IndicatorEntityMapper mapper;

    public IndicatorRepositoryAdapter(JpaIndicatorRepository repository, IndicatorEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Indicator save(Indicator indicator) {
        IndicatorEntity parent = resolveParent(indicator.parentId());
        IndicatorEntity entity = indicator.id() == null
                ? repository.findByCodeAndIndicatorGroupCodeAndParentId(
                indicator.code(),
                indicator.groupCode(),
                indicator.parentId()
        ).orElseGet(() -> mapper.toNewEntity(indicator, parent))
                : repository.findById(indicator.id()).orElseGet(() -> mapper.toNewEntity(indicator, parent));

        mapper.copyToEntity(indicator, parent, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Indicator> findByCodeAndGroupCode(String code, IndicatorGroupCode groupCode) {
        return repository.findByCodeAndIndicatorGroupCode(code, groupCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<Indicator> findAllByGroupCode(IndicatorGroupCode groupCode) {
        return repository.findAllByIndicatorGroupCodeOrderBySortOrderAscNameAsc(groupCode).stream()
                .map(mapper::toDomain)
                .toList();
    }

    private IndicatorEntity resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return repository.findById(parentId)
                .orElseThrow(() -> new IllegalStateException("Parent indicator not found: " + parentId));
    }
}
