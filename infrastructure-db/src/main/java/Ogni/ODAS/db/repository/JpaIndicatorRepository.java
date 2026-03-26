package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.IndicatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaIndicatorRepository extends JpaRepository<IndicatorEntity, Long> {

    Optional<IndicatorEntity> findByCode(String code);
}
