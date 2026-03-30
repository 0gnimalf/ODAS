package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.mapper.IndicatorEntityMapper;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class IndicatorRepositoryAdapter implements IndicatorRepositoryPort {

    private final JpaIndicatorRepository repository;

    public IndicatorRepositoryAdapter(JpaIndicatorRepository repository) {
        this.repository = repository;
    }

    @Override
    public Indicator save(Indicator indicator) {
        IndicatorEntity entity = indicator.id() == null
                ? repository.findByCodeAndIndicatorGroupCode(indicator.code(), indicator.groupCode()).orElseGet(IndicatorEntity::new)
                : repository.findById(indicator.id()).orElseGet(IndicatorEntity::new);

        entity.setCode(indicator.code());
        entity.setName(indicator.name());
        entity.setIndicatorGroupCode(indicator.groupCode());
        entity.setLevel(indicator.level());
        entity.setSortOrder(indicator.sortOrder());
        entity.setSection(indicator.section());
        entity.setParent(resolveParent(indicator.parentId()));

        return IndicatorEntityMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Indicator> findByCodeAndGroupCode(String code, IndicatorGroupCode groupCode) {
        return repository.findByCodeAndIndicatorGroupCode(code, groupCode)
                .map(IndicatorEntityMapper::toDomain);
    }

    @Override
    public List<Indicator> findAllByGroupCode(IndicatorGroupCode groupCode) {
        return repository.findAllByIndicatorGroupCodeOrderBySortOrderAscNameAsc(groupCode).stream()
                .map(IndicatorEntityMapper::toDomain)
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
