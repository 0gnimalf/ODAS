package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.CollectObservationsCommand;
import Ogni.ODAS.application.dto.ObservationCollectionResultDto;

public interface ObservationCollectionUseCase {

    ObservationCollectionResultDto collectMonthlyObservations(CollectObservationsCommand command);
}
