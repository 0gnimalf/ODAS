package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.Region;

import java.util.List;

public interface ReferenceCatalogUseCase {

    ReferenceSyncResultDto syncRegions();

    ReferenceSyncResultDto syncIndicators(SyncIndicatorsCommand command);

    List<Region> getRegions();

    List<Indicator> getIndicators(IndicatorGroupCode groupCode);
}
