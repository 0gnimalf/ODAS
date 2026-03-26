package Ogni.ODAS.application.port.out;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedObservationDto;

import java.util.List;

public interface ExternalSourceCollectorPort {

    List<CollectedObservationDto> collect(AnalyzeBudgetDataCommand command);
}
