package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.model.Region;

import java.util.List;
import java.util.Optional;

public interface RegionRepositoryPort {

    Region save(Region region);

    Optional<Region> findByCode(String code);

    List<Region> saveAll(List<Region> regions);

    List<Region> findAll();
}
