package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import Ogni.ODAS.db.iminfin.entity.IminfinParameterOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaIminfinParameterOptionRepository extends JpaRepository<IminfinParameterOptionEntity, Long> {

    List<IminfinParameterOptionEntity> findAllByProfileKeyAndParameterNameOrderByExternalValue(
            IminfinReportProfileKey profileKey,
            String parameterName
    );

    Optional<IminfinParameterOptionEntity> findByProfileKeyAndParameterNameAndExternalValue(
            IminfinReportProfileKey profileKey,
            String parameterName,
            String externalValue
    );
}
