package Ogni.ODAS.db.iminfin.repository;

import Ogni.ODAS.db.iminfin.entity.IminfinRegionMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaIminfinRegionMappingRepository extends JpaRepository<IminfinRegionMappingEntity, Long> {

    Optional<IminfinRegionMappingEntity> findByExternalTerritoryCode(String externalTerritoryCode);
}
