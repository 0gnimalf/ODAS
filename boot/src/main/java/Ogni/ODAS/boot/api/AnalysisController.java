//package Ogni.ODAS.boot.api;
//
//import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
//import Ogni.ODAS.application.dto.AnalysisResultDto;
//import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
//import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/analysis")
//public class AnalysisController {
//
//    private final AnalyzeBudgetDataUseCase analyzeBudgetDataUseCase;
//
//    public AnalysisController(AnalyzeBudgetDataUseCase analyzeBudgetDataUseCase) {
//        this.analyzeBudgetDataUseCase = analyzeBudgetDataUseCase;
//    }
//
//    @GetMapping
//    public List<AnalysisResultDto> analyze(
//            @RequestParam(name = "regionCode") String regionCode,
//            @RequestParam(name = "groupCode") IndicatorGroupCode groupCode,
//            @RequestParam(name = "indicatorCode") String indicatorCode,
//            @RequestParam(name = "year") Integer year,
//            @RequestParam(name = "month") Integer month,
//            @RequestParam(name = "forceRefresh", defaultValue = "false") boolean forceRefresh
//    ) {
//        AnalyzeBudgetDataCommand command = new AnalyzeBudgetDataCommand(
//                regionCode,
//                groupCode,
//                indicatorCode,
//                year,
//                month,
//                forceRefresh
//        );
//
//        return analyzeBudgetDataUseCase.analyze(command);
//    }
//}
