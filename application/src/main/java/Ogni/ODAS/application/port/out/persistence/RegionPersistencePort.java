package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.model.Region;

import java.util.List;
import java.util.Optional;

public interface RegionPersistencePort {

    Region save(Region region);

    Optional<Region> findByCode(String code);

    boolean existsAny();

    List<Region> findAll();
}
