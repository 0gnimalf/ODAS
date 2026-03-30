package Ogni.ODAS.application.port.out;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;

public interface ExternalSourceCollectorPort {

    CollectedDatasetDto collect(AnalyzeBudgetDataCommand command);
}
