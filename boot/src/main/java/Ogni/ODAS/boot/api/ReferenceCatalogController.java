package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.command.SyncIndicatorsCommand;
import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceCatalogUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.Region;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/references")
public class ReferenceCatalogController {

    private final ReferenceCatalogUseCase referenceCatalogUseCase;

    public ReferenceCatalogController(ReferenceCatalogUseCase referenceCatalogUseCase) {
        this.referenceCatalogUseCase = referenceCatalogUseCase;
    }

    @GetMapping("/sync/regions")
    public ReferenceSyncResultDto syncRegions() {
        return referenceCatalogUseCase.syncRegions();
    }

    @GetMapping("/sync/indicators")
    public ReferenceSyncResultDto syncIndicators(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "groupCode", required = false) IndicatorGroupCode groupCode
    ) {
        return referenceCatalogUseCase.syncIndicators(new SyncIndicatorsCommand(groupCode, year));
    }

    @GetMapping("/regions")
    public List<Region> regions() {
        return referenceCatalogUseCase.getRegions();
    }

    @GetMapping("/indicators")
    public List<Indicator> indicators(@RequestParam(name = "groupCode") IndicatorGroupCode groupCode) {
        return referenceCatalogUseCase.getIndicators(groupCode);
    }
}
