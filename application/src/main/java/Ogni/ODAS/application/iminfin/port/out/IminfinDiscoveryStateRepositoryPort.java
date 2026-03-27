package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryState;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;

import java.util.Optional;

public interface IminfinDiscoveryStateRepositoryPort {

    Optional<IminfinDiscoveryState> findByProfileKey(IminfinReportProfileKey profileKey);

    IminfinDiscoveryState save(IminfinDiscoveryState state);
}
