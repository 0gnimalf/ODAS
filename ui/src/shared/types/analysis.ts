import type {IndicatorGroupCode} from './read';

export type ObservationValueKind =
    | 'PLAN'
    | 'REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET'
    | 'REFINED_PLAN_SUBJECT_BUDGET'
    | 'REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION'
    | 'ACTUAL_CONSOLIDATED_SUBJECT_BUDGET'
    | 'ACTUAL_SUBJECT_BUDGET'
    // | 'ACTUAL_RATE_TO_PREVIOUS_PERIOD'
    | 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT'
    | 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT'
    | 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION'
// | 'POPULATION'
// | 'SHARE'
// | 'PER_CAPITA';
    ;

export type NonCumulativeValueMode = 'SERIES_RANGE' | 'TARGET_MONTH_AND_QUARTER_METRICS';

export const OBSERVATION_VALUE_KIND_OPTIONS: Array<{ code: ObservationValueKind; label: string }> = [
    {code: 'PLAN', label: 'План'},
    {code: 'REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET', label: 'Уточненный план; конс.бюджет субъекта РФ'},
    {code: 'REFINED_PLAN_SUBJECT_BUDGET', label: 'Уточненный план; в т.ч. бюджет субъекта'},
    {
        code: 'REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION',
        label: 'Темп уточненного плана к исполнению пред. года (кроме кредитов)'
    },
    {code: 'ACTUAL_CONSOLIDATED_SUBJECT_BUDGET', label: 'Исполнено; конс.бюджет субъекта РФ'},
    {code: 'ACTUAL_SUBJECT_BUDGET', label: 'Исполнено; в т.ч. бюджет субъекта'},
    // {code: 'ACTUAL_RATE_TO_PREVIOUS_PERIOD', label: 'Исполнено; темп к соотв. периоду пред. года'},
    {code: 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT', label: 'Темп роста; по субъекту (кроме кредитов)'},
    {code: 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT', label: 'Темп роста; по ФО (кроме кредитов)'},
    {code: 'GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION', label: 'Темп роста; по РФ (кроме кредитов)'},
    // {code: 'POPULATION', label: 'Численность населения'},
    // {code: 'SHARE', label: 'Доля'},
    // {code: 'PER_CAPITA', label: 'На душу населения'}
];

export interface BuildMonthlySeriesCommand {
    groupCode: IndicatorGroupCode;
    regionId: number;
    indicatorYearEntryId: number;
    valueKind: ObservationValueKind;
    year: number;
    month: number;
    includeQuarterAggregates: boolean;
    autoCollectMissing: boolean;
    forceRefresh: boolean;
}

export interface CalculatePeriodGrowthMetricsCommand {
    groupCode: IndicatorGroupCode;
    regionId: number;
    indicatorYearEntryId: number;
    valueKind: ObservationValueKind;
    year: number;
    month: number;
    autoCollectMissing: boolean;
    forceRefresh: boolean;
}

export interface CompareRegionsCommand {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    indicatorYearEntryId: number;
    valueKind: ObservationValueKind;
    regionIds: number[];
    forceRefresh: boolean;
}

export interface BuildSubtreeSliceCommand {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    regionId: number;
    rootIndicatorYearEntryId: number;
    valueKind: ObservationValueKind;
    forceRefresh: boolean;
}

export interface BuildRegionIndicatorMatrixCommand {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    regionIds: number[];
    indicatorYearEntryIds: number[];
    valueKind: ObservationValueKind;
    forceRefresh: boolean;
}

export interface MonthlySeriesPointDto {
    periodId: number;
    year: number;
    month: number;
    periodLabel: string;
    cumulativeValue: number | null;
    nonCumulativeValue: number | null;
    nonCumulativeCalculated: boolean;
    anomaly: boolean;
}

export interface QuarterAggregateDto {
    year: number;
    quarter: number;
    label: string;
    aggregatedValue: number | null;
    coveredMonthCount: number;
    complete: boolean;
}

export interface MonthlySeriesResultDto {
    groupCode: IndicatorGroupCode;
    regionId: number;
    regionName: string;
    indicatorYearEntryId: number;
    indicatorName: string;
    valueKind: ObservationValueKind;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    nonCumulativeMode: NonCumulativeValueMode;
    targetYear: number;
    targetMonth: number;
    expectedMonthCount: number;
    availableMonthCount: number;
    autoCollectedMissing: boolean;
    points: MonthlySeriesPointDto[];
    quarterAggregates: QuarterAggregateDto[];
}

export interface PeriodGrowthMetricsResultDto {
    groupCode: IndicatorGroupCode;
    regionId: number;
    regionName: string;
    indicatorYearEntryId: number;
    indicatorName: string;
    valueKind: ObservationValueKind;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    targetYear: number;
    targetMonth: number;
    targetMonthPoint: MonthlySeriesPointDto | null;
    previousMonthPoint: MonthlySeriesPointDto | null;
    sameMonthPreviousYearPoint: MonthlySeriesPointDto | null;
    absoluteDeltaToPreviousMonth: number | null;
    rateToPreviousMonthPercent: number | null;
    absoluteDeltaToSameMonthPreviousYear: number | null;
    rateToSameMonthPreviousYearPercent: number | null;
    currentQuarter: QuarterAggregateDto | null;
    previousQuarter: QuarterAggregateDto | null;
    sameQuarterPreviousYear: QuarterAggregateDto | null;
    absoluteDeltaToPreviousQuarter: number | null;
    rateToPreviousQuarterPercent: number | null;
    absoluteDeltaToSameQuarterPreviousYear: number | null;
    rateToSameQuarterPreviousYearPercent: number | null;
    autoCollectedMissing: boolean;
}

export interface RegionComparisonItemDto {
    regionId: number;
    regionName: string;
    value: number | null;
    missing: boolean;
    rank: number | null;
    shareOfTotalPercent: number | null;
    deltaFromLeader: number | null;
    deltaFromAverage: number | null;
}

export interface RegionComparisonSummaryDto {
    requestedRegionCount: number;
    foundRegionCount: number;
    minValue: number | null;
    maxValue: number | null;
    averageValue: number | null;
    medianValue: number | null;
    totalValue: number | null;
}

export interface RegionComparisonResultDto {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    indicatorYearEntryId: number;
    indicatorName: string;
    valueKind: ObservationValueKind;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    summary: RegionComparisonSummaryDto;
    items: RegionComparisonItemDto[];
}

export interface SubtreeSliceNodeDto {
    indicatorYearEntryId: number;
    indicatorId: number;
    indicatorName: string;
    parentIndicatorYearEntryId: number | null;
    level: number;
    sortOrder: number | null;
    hasChildren: boolean;
    path: string;
    value: number | null;
    missing: boolean;
    shareOfParentPercent: number | null;
    shareOfRootPercent: number | null;
}

export interface SubtreeSliceResultDto {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    regionId: number;
    regionName: string;
    rootIndicatorYearEntryId: number;
    rootIndicatorName: string;
    valueKind: ObservationValueKind;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    nodes: SubtreeSliceNodeDto[];
}

export interface RegionIndicatorMatrixRowDto {
    regionId: number;
    regionName: string;
}

export interface RegionIndicatorMatrixColumnDto {
    indicatorYearEntryId: number;
    indicatorId: number;
    indicatorName: string;
    level: number;
    sortOrder: number | null;
}

export interface MatrixCellDto {
    regionId: number;
    indicatorYearEntryId: number;
    value: number | null;
    missing: boolean;
}

export interface RegionIndicatorMatrixResultDto {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    valueKind: ObservationValueKind;
    valueKindLabel: string;
    unitCode: string;
    unitCodeLabel: string;
    rows: RegionIndicatorMatrixRowDto[];
    columns: RegionIndicatorMatrixColumnDto[];
    cells: MatrixCellDto[];
}
