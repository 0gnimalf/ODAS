package Ogni.ODAS.db.repository;

import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionJpaRepository extends JpaRepository<RegionEntity, Long> {

    Optional<RegionEntity> findByCode(String code);

    List<RegionEntity> findAllByFederalDistrictCodeOrderByNameAsc(FederalDistrictCode federalDistrictCode);

    List<RegionEntity> findAllByNameContainingIgnoreCaseOrderByNameAsc(String namePart);
}
