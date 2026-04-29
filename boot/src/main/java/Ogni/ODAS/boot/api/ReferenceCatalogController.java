package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceSyncUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference")
public class ReferenceCatalogController {

    private final ReferenceSyncUseCase referenceSyncUseCase;

    public ReferenceCatalogController(ReferenceSyncUseCase referenceSyncUseCase) {
        this.referenceSyncUseCase = referenceSyncUseCase;
    }

    @PostMapping("/regions/sync")
    public ReferenceSyncResultDto syncRegions(
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        return force
                ? referenceSyncUseCase.syncRegions()
                : referenceSyncUseCase.syncRegionsIfNecessary();
    }

    @PostMapping("/indicators/sync")
    public ReferenceSyncResultDto syncIndicators(
            @RequestParam("year") Integer year,
            @RequestParam(name = "group", required = false) IndicatorGroupCode groupCode
    ) {
        return referenceSyncUseCase.syncIndicators(new SyncIndicatorsCommand(groupCode, year));
    }
}
