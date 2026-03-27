package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.model.IminfinParameterCatalogItem;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.application.iminfin.port.out.IminfinParameterCatalogRepositoryPort;
import Ogni.ODAS.db.iminfin.mapper.IminfinParameterCatalogEntityMapper;
import Ogni.ODAS.db.iminfin.repository.JpaIminfinParameterCatalogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class IminfinParameterCatalogRepositoryAdapter implements IminfinParameterCatalogRepositoryPort {

    private final JpaIminfinParameterCatalogRepository repository;

    public IminfinParameterCatalogRepositoryAdapter(JpaIminfinParameterCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<IminfinParameterCatalogItem> findByProfileKey(IminfinReportProfileKey profileKey) {
        return repository.findAllByProfileKeyOrderByParameterName(profileKey)
                .stream()
                .map(IminfinParameterCatalogEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void replaceForProfile(IminfinReportProfileKey profileKey, List<IminfinParameterCatalogItem> items) {
        repository.deleteAllByProfileKey(profileKey);
        repository.saveAll(items.stream()
                .map(IminfinParameterCatalogEntityMapper::toEntity)
                .toList());
    }
}
