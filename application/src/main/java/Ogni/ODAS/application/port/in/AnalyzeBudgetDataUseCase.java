package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.AnalysisResultDto;

import java.util.List;

public interface AnalyzeBudgetDataUseCase {

    List<AnalysisResultDto> analyze(AnalyzeBudgetDataCommand command);
}
