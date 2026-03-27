package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinDiscoveryStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaIminfinDiscoveryStateRepository extends JpaRepository<IminfinDiscoveryStateEntity, Long> {

    Optional<IminfinDiscoveryStateEntity> findByProfileKey(IminfinReportProfileKey profileKey);
}
