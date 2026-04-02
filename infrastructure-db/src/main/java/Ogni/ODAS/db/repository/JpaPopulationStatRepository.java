package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.PopulationStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPopulationStatRepository extends JpaRepository<PopulationStatEntity, Long> {

    Optional<PopulationStatEntity> findByRegionCodeAndReportingPeriodYear(
            String regionCode,
            Integer year
    );
}
