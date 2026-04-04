package Ogni.ODAS.application.port.out;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;

import java.util.List;

public interface ExternalReferenceCollectorPort {

    List<CollectedRegionDto> collectRegions();

    List<CollectedIndicatorDto> collectIndicators(SyncIndicatorsCommand command);
}
