package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.command.ReadObservationsCommand;
import Ogni.ODAS.application.dto.read.IndicatorGroupReadDto;
import Ogni.ODAS.application.dto.read.IndicatorTreeNodeReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadResultDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.in.StoredDataReadUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/read")
public class ReadController {

    private final StoredDataReadUseCase storedDataReadUseCase;

    public ReadController(StoredDataReadUseCase storedDataReadUseCase) {
        this.storedDataReadUseCase = storedDataReadUseCase;
    }

    @GetMapping("/groups")
    public List<IndicatorGroupReadDto> groups() {
        return storedDataReadUseCase.getIndicatorGroups();
    }

    @GetMapping("/regions")
    public List<RegionReadDto> regions() {
        return storedDataReadUseCase.getRegions();
    }

    @GetMapping("/indicators/tree")
    public List<IndicatorTreeNodeReadDto> indicatorTree(
            @RequestParam("group") IndicatorGroupCode groupCode,
            @RequestParam("year") Integer year
    ) {
        return storedDataReadUseCase.getIndicatorTree(groupCode, year);
    }

    @GetMapping("/observations")
    public ObservationReadResultDto observations(
            @RequestParam("group") IndicatorGroupCode groupCode,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(name = "regionId") List<Long> regionIds,
            @RequestParam(name = "indicatorYearEntryId") List<Long> indicatorYearEntryIds,
            @RequestParam(name = "valueKind", required = false) Set<ObservationValueKind> valueKinds,
            @RequestParam(name = "includeChildren", defaultValue = "false") boolean includeChildren,
            @RequestParam(name = "forceRefresh", defaultValue = "false") boolean forceRefresh
    ) {
        return storedDataReadUseCase.getObservations(new ReadObservationsCommand(
                groupCode,
                year,
                month,
                regionIds,
                indicatorYearEntryIds,
                valueKinds,
                includeChildren,
                forceRefresh
        ));
    }
}
