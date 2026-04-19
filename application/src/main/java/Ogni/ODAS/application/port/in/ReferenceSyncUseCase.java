package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;

public interface ReferenceSyncUseCase {

    ReferenceSyncResultDto syncRegionsIfNecessary();

    ReferenceSyncResultDto syncRegions();

    ReferenceSyncResultDto syncIndicators(SyncIndicatorsCommand command);
}
