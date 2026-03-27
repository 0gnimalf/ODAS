package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinReportSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaIminfinReportSessionRepository extends JpaRepository<IminfinReportSessionEntity, Long> {

    Optional<IminfinReportSessionEntity> findFirstByProfileKeyOrderByResolvedAtDesc(IminfinReportProfileKey profileKey);
}
