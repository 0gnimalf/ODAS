import {apiPost} from './httpClient';
import type {
    BuildMonthlySeriesRequest,
    BuildRegionIndicatorMatrixRequest,
    BuildSubtreeSliceRequest,
    CalculatePeriodGrowthMetricsRequest,
    CompareRegionsRequest,
    MonthlySeriesResultDto,
    PeriodGrowthMetricsResultDto,
    RegionComparisonResultDto,
    RegionIndicatorMatrixResultDto,
    SubtreeSliceResultDto
} from '../types/analysis';

const ANALYSIS_API_ROOT = '/api/analysis';

export const buildMonthlySeries = (request: BuildMonthlySeriesRequest) =>
    apiPost<BuildMonthlySeriesRequest, MonthlySeriesResultDto>(`${ANALYSIS_API_ROOT}/series/monthly`, request);

export const calculatePeriodGrowthMetrics = (request: CalculatePeriodGrowthMetricsRequest) =>
    apiPost<CalculatePeriodGrowthMetricsRequest, PeriodGrowthMetricsResultDto>(`${ANALYSIS_API_ROOT}/metrics/period-growth`, request);

export const compareRegions = (request: CompareRegionsRequest) =>
    apiPost<CompareRegionsRequest, RegionComparisonResultDto>(`${ANALYSIS_API_ROOT}/compare/regions`, request);

export const buildSubtreeSlice = (request: BuildSubtreeSliceRequest) =>
    apiPost<BuildSubtreeSliceRequest, SubtreeSliceResultDto>(`${ANALYSIS_API_ROOT}/subtree`, request);

export const buildRegionIndicatorMatrix = (request: BuildRegionIndicatorMatrixRequest) =>
    apiPost<BuildRegionIndicatorMatrixRequest, RegionIndicatorMatrixResultDto>(`${ANALYSIS_API_ROOT}/matrix`, request);
