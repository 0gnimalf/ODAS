package Ogni.ODAS.boot.temp;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceSyncUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/temp/reference")
public class TemporaryReferenceController {

    private final ReferenceSyncUseCase referenceSyncUseCase;

    public TemporaryReferenceController(ReferenceSyncUseCase referenceSyncUseCase) {
        this.referenceSyncUseCase = referenceSyncUseCase;
    }

    @GetMapping("/regions")
    public ReferenceSyncResultDto syncRegions(
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        return force
                ? referenceSyncUseCase.syncRegions()
                : referenceSyncUseCase.syncRegionsIfNecessary();
    }

    @GetMapping("/indicators")
    public ReferenceSyncResultDto syncIndicators(
            @RequestParam("year") Integer year,
            @RequestParam(name = "group", required = false) IndicatorGroupCode groupCode
    ) {
        return referenceSyncUseCase.syncIndicators(new SyncIndicatorsCommand(groupCode, year));
    }
}
