import {API_BASE_URL} from '../config/env';
import type {
    BuildMonthlySeriesCommand,
    BuildRegionIndicatorMatrixCommand,
    BuildSubtreeSliceCommand,
    CalculatePeriodGrowthMetricsCommand,
    CompareRegionsCommand,
    MonthlySeriesResultDto,
    PeriodGrowthMetricsResultDto,
    RegionComparisonResultDto,
    RegionIndicatorMatrixResultDto,
    SubtreeSliceResultDto
} from '../types/analysis';

async function postJson<TReq, TRes>(path: string, body: TReq): Promise<TRes> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
    }
    return await response.json() as TRes;
}

export const buildMonthlySeries = (command: BuildMonthlySeriesCommand) => postJson<BuildMonthlySeriesCommand, MonthlySeriesResultDto>('/internal/temp/analysis/series/monthly', command);
export const calculatePeriodGrowthMetrics = (command: CalculatePeriodGrowthMetricsCommand) => postJson<CalculatePeriodGrowthMetricsCommand, PeriodGrowthMetricsResultDto>('/internal/temp/analysis/metrics/period-growth', command);
export const compareRegions = (command: CompareRegionsCommand) => postJson<CompareRegionsCommand, RegionComparisonResultDto>('/internal/temp/analysis/compare/regions', command);
export const buildSubtreeSlice = (command: BuildSubtreeSliceCommand) => postJson<BuildSubtreeSliceCommand, SubtreeSliceResultDto>('/internal/temp/analysis/subtree', command);
export const buildRegionIndicatorMatrix = (command: BuildRegionIndicatorMatrixCommand) => postJson<BuildRegionIndicatorMatrixCommand, RegionIndicatorMatrixResultDto>('/internal/temp/analysis/matrix', command);
