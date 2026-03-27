package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterCatalogRepositoryPort;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterCatalogEntity;
import Ogni.ODAS.db.iminfin.mapper.IminfinParameterCatalogEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinParameterCatalogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IminfinParameterCatalogRepositoryAdapter implements IminfinParameterCatalogRepositoryPort {

    private final JpaIminfinParameterCatalogRepository repository;

    public IminfinParameterCatalogRepositoryAdapter(JpaIminfinParameterCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IminfinParameterCatalogItem> findByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findAllByProfileKeyOrderByParameterName(profileKey)
                .stream()
                .map(IminfinParameterCatalogEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void replaceForProfile(IminfinReportProfileKey profileKey, List<IminfinParameterCatalogItem> items) {
        Map<String, IminfinParameterCatalogItem> incomingByName = deduplicate(profileKey, items);

        List<IminfinParameterCatalogEntity> existing = repository.findAllByProfileKeyOrderByParameterName(profileKey);
        Map<String, IminfinParameterCatalogEntity> existingByName = existing.stream()
                .collect(Collectors.toMap(
                        IminfinParameterCatalogEntity::getParameterName,
                        entity -> entity,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<String> incomingNames = incomingByName.keySet();

        List<IminfinParameterCatalogEntity> stale = existing.stream()
                .filter(entity -> !incomingNames.contains(entity.getParameterName()))
                .toList();
        if (!stale.isEmpty()) {
            repository.deleteAll(stale);
        }

        OffsetDateTime now = OffsetDateTime.now();

        for (IminfinParameterCatalogItem item : incomingByName.values()) {
            IminfinParameterCatalogEntity entity = existingByName.get(item.parameterName());
            if (entity == null) {
                entity = new IminfinParameterCatalogEntity();
                entity.setProfileKey(profileKey);
                entity.setParameterName(item.parameterName());
            }

            entity.setRole(item.role());
            entity.setValueType(item.valueType());
            entity.setRequired(item.required());
            entity.setDefaultValue(item.defaultValue());
            entity.setLastSeenAt(now);

            repository.save(entity);
        }
    }

    private Map<String, IminfinParameterCatalogItem> deduplicate(
            IminfinReportProfileKey profileKey,
            List<IminfinParameterCatalogItem> items
    ) {
        Map<String, IminfinParameterCatalogItem> deduplicated = new LinkedHashMap<>();

        for (IminfinParameterCatalogItem item : items) {
            if (item == null || item.parameterName() == null || item.parameterName().isBlank()) {
                continue;
            }

            IminfinParameterCatalogItem normalized = new IminfinParameterCatalogItem(
                    item.id(),
                    profileKey,
                    item.parameterName(),
                    item.role(),
                    item.valueType(),
                    item.required(),
                    item.defaultValue()
            );

            deduplicated.put(item.parameterName(), normalized);
        }

        return deduplicated;
    }
}