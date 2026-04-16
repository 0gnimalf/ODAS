package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicatorJpaRepository extends JpaRepository<IndicatorEntity, Long> {

    Optional<IndicatorEntity> findByNameAndIndicatorGroupCode(String name, IndicatorGroupCode indicatorGroupCode);

    List<IndicatorEntity> findAllByIndicatorGroupCodeOrderByNameAsc(IndicatorGroupCode indicatorGroupCode);
}
