package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.command.analysis.*;
import Ogni.ODAS.application.dto.analysis.*;
import Ogni.ODAS.application.port.in.AnalysisUseCase;
import Ogni.ODAS.boot.api.request.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisUseCase analysisUseCase;

    public AnalysisController(AnalysisUseCase analysisUseCase) {
        this.analysisUseCase = analysisUseCase;
    }

    @PostMapping("/compare/regions")
    public RegionComparisonResultDto compareRegions(@RequestBody CompareRegionsRequest request) {
        return analysisUseCase.compareRegions(new CompareRegionsCommand(
                request.groupCode(),
                request.year(),
                request.month(),
                request.indicatorYearEntryId(),
                request.valueKind(),
                request.regionIds(),
                request.forceRefresh()
        ));
    }

    @PostMapping("/series/monthly")
    public MonthlySeriesResultDto monthlySeries(@RequestBody BuildMonthlySeriesRequest request) {
        return analysisUseCase.buildMonthlySeries(new BuildMonthlySeriesCommand(
                request.groupCode(),
                request.regionId(),
                request.indicatorYearEntryId(),
                request.valueKind(),
                request.year(),
                request.month(),
                request.includeQuarterAggregates(),
                request.autoCollectMissing(),
                request.forceRefresh()
        ));
    }

    @PostMapping("/metrics/period-growth")
    public PeriodGrowthMetricsResultDto periodGrowthMetrics(@RequestBody CalculatePeriodGrowthMetricsRequest request) {
        return analysisUseCase.calculatePeriodGrowthMetrics(new CalculatePeriodGrowthMetricsCommand(
                request.groupCode(),
                request.regionId(),
                request.indicatorYearEntryId(),
                request.valueKind(),
                request.year(),
                request.month(),
                request.autoCollectMissing(),
                request.forceRefresh()
        ));
    }

    @PostMapping("/subtree")
    public SubtreeSliceResultDto subtree(@RequestBody BuildSubtreeSliceRequest request) {
        return analysisUseCase.buildSubtreeSlice(new BuildSubtreeSliceCommand(
                request.groupCode(),
                request.year(),
                request.month(),
                request.regionId(),
                request.rootIndicatorYearEntryId(),
                request.valueKind(),
                request.forceRefresh()
        ));
    }

    @PostMapping("/matrix")
    public RegionIndicatorMatrixResultDto matrix(@RequestBody BuildRegionIndicatorMatrixRequest request) {
        return analysisUseCase.buildRegionIndicatorMatrix(new BuildRegionIndicatorMatrixCommand(
                request.groupCode(),
                request.year(),
                request.month(),
                request.regionIds(),
                request.indicatorYearEntryIds(),
                request.valueKind(),
                request.forceRefresh()
        ));
    }
}
