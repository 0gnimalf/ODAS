package Ogni.ODAS.boot.temp;

import Ogni.ODAS.application.command.analysis.*;
import Ogni.ODAS.application.dto.analysis.*;
import Ogni.ODAS.application.port.in.AnalysisUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/temp/analysis")
public class TemporaryAnalysisController {

    private final AnalysisUseCase analysisUseCase;

    public TemporaryAnalysisController(AnalysisUseCase analysisUseCase) {
        this.analysisUseCase = analysisUseCase;
    }

    @PostMapping("/compare/regions")
    public RegionComparisonResultDto compareRegions(@RequestBody CompareRegionsCommand command) {
        return analysisUseCase.compareRegions(command);
    }

    @PostMapping("/series/monthly")
    public MonthlySeriesResultDto monthlySeries(@RequestBody BuildMonthlySeriesCommand command) {
        return analysisUseCase.buildMonthlySeries(command);
    }

    @PostMapping("/metrics/period-growth")
    public PeriodGrowthMetricsResultDto periodGrowthMetrics(@RequestBody CalculatePeriodGrowthMetricsCommand command) {
        return analysisUseCase.calculatePeriodGrowthMetrics(command);
    }

    @PostMapping("/subtree")
    public SubtreeSliceResultDto subtree(@RequestBody BuildSubtreeSliceCommand command) {
        return analysisUseCase.buildSubtreeSlice(command);
    }

    @PostMapping("/matrix")
    public RegionIndicatorMatrixResultDto matrix(@RequestBody BuildRegionIndicatorMatrixCommand command) {
        return analysisUseCase.buildRegionIndicatorMatrix(command);
    }
}
