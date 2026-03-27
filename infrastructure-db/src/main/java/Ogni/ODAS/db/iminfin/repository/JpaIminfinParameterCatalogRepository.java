package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaIminfinParameterCatalogRepository extends JpaRepository<IminfinParameterCatalogEntity, Long> {

    List<IminfinParameterCatalogEntity> findAllByProfileKeyOrderByParameterName(IminfinReportProfileKey profileKey);

    void deleteAllByProfileKey(IminfinReportProfileKey profileKey);
}
