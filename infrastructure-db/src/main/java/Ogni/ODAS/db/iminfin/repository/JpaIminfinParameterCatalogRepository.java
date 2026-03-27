package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaIminfinParameterCatalogRepository extends JpaRepository<IminfinParameterCatalogEntity, Long> {

    List<IminfinParameterCatalogEntity> findAllByProfileKeyOrderByParameterName(
            IminfinReportProfileKey profileKey
    );

    Optional<IminfinParameterCatalogEntity> findByProfileKeyAndParameterName(
            IminfinReportProfileKey profileKey,
            String parameterName
    );
}