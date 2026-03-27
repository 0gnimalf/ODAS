package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.domain.model.Region;

import java.util.Optional;

public interface RegionDictionaryRepositoryPort {

    Optional<Region> findByCode(String code);

    Optional<Region> findByNameIgnoreCase(String name);
}
