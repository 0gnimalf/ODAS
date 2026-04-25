package Ogni.ODAS.application.port.in;

import Ogni.ODAS.application.command.analysis.*;
import Ogni.ODAS.application.dto.analysis.*;

public interface AnalysisUseCase {

    RegionComparisonResultDto compareRegions(CompareRegionsCommand command);

    MonthlySeriesResultDto buildMonthlySeries(BuildMonthlySeriesCommand command);

    PeriodGrowthMetricsResultDto calculatePeriodGrowthMetrics(CalculatePeriodGrowthMetricsCommand command);

    SubtreeSliceResultDto buildSubtreeSlice(BuildSubtreeSliceCommand command);

    RegionIndicatorMatrixResultDto buildRegionIndicatorMatrix(BuildRegionIndicatorMatrixCommand command);
}
