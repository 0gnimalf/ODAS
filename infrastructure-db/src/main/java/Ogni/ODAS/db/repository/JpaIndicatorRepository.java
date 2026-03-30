package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaIndicatorRepository extends JpaRepository<IndicatorEntity, Long> {

    Optional<IndicatorEntity> findByCodeAndIndicatorGroupCode(String code, IndicatorGroupCode groupCode);

    List<IndicatorEntity> findAllByIndicatorGroupCodeOrderBySortOrderAscNameAsc(IndicatorGroupCode groupCode);
}
