package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinReportSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaIminfinReportSessionRepository extends JpaRepository<IminfinReportSessionEntity, Long> {

    Optional<IminfinReportSessionEntity> findByProfileKeyAndDataVersion(
            IminfinReportProfileKey profileKey,
            String dataVersion
    );

    List<IminfinReportSessionEntity> findAllByProfileKeyOrderByResolvedAtDesc(
            IminfinReportProfileKey profileKey
    );

    Optional<IminfinReportSessionEntity> findFirstByProfileKeyOrderByResolvedAtDesc(
            IminfinReportProfileKey profileKey
    );
}