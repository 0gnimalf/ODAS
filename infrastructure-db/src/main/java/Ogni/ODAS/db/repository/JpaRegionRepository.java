package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.RegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaRegionRepository extends JpaRepository<RegionEntity, Long> {

    Optional<RegionEntity> findByCode(String code);

    List<RegionEntity> findAllByCodeIn(Collection<String> codes);
}
