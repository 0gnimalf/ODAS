package Ogni.ODAS.application.port.out;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;

import java.util.List;

public interface ExternalSourceCollectorPort {

    CollectedDatasetDto collect(AnalyzeBudgetDataCommand command);

    List<Integer> getDesiredObservationIndexes();
}
