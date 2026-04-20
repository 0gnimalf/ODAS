package Ogni.ODAS.boot.temp;

import Ogni.ODAS.application.command.CollectObservationsCommand;
import Ogni.ODAS.application.dto.ObservationCollectionResultDto;
import Ogni.ODAS.application.port.in.ObservationCollectionUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/temp/observations")
public class TemporaryObservationController {

    private final ObservationCollectionUseCase observationCollectionUseCase;

    public TemporaryObservationController(ObservationCollectionUseCase observationCollectionUseCase) {
        this.observationCollectionUseCase = observationCollectionUseCase;
    }

    @GetMapping("/collect")
    public ObservationCollectionResultDto collect(
            @RequestParam("group") IndicatorGroupCode groupCode,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(name = "regionId", required = false) List<Long> regionIds
    ) {
        return observationCollectionUseCase.collectMonthlyObservations(new CollectObservationsCommand(groupCode, year, month, regionIds));
    }
}
