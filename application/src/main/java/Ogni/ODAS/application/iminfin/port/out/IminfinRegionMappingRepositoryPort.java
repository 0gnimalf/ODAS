package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinRegionMapping;

import java.util.Optional;

public interface IminfinRegionMappingRepositoryPort {

    Optional<IminfinRegionMapping> findByExternalTerritoryCode(String externalTerritoryCode);

    IminfinRegionMapping save(IminfinRegionMapping mapping);
}
