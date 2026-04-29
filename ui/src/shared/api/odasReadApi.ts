import {apiGet} from './httpClient';
import {appendRepeatedNumberParams} from '../lib/queryParams';
import type {
    IndicatorGroupCode,
    IndicatorGroupReadDto,
    IndicatorTreeNodeReadDto,
    ObservationQuery,
    ObservationReadResultDto,
    RegionReadDto
} from '../types/read';

const READ_API_ROOT = '/api/read';

export function getIndicatorGroups(): Promise<IndicatorGroupReadDto[]> {
    return apiGet<IndicatorGroupReadDto[]>(`${READ_API_ROOT}/groups`);
}

export function getRegions(): Promise<RegionReadDto[]> {
    return apiGet<RegionReadDto[]>(`${READ_API_ROOT}/regions`);
}

export function getIndicatorTree(
    groupCode: IndicatorGroupCode,
    year: number
): Promise<IndicatorTreeNodeReadDto[]> {
    const params = new URLSearchParams({
        group: groupCode,
        year: String(year)
    });
    return apiGet<IndicatorTreeNodeReadDto[]>(`${READ_API_ROOT}/indicators/tree?${params.toString()}`);
}

export function getObservations(query: ObservationQuery): Promise<ObservationReadResultDto> {
    const params = new URLSearchParams({
        group: query.groupCode,
        year: String(query.year),
        month: String(query.month),
        includeChildren: String(query.includeChildren),
        forceRefresh: String(query.forceRefresh)
    });

    appendRepeatedNumberParams(params, 'regionId', query.regionIds);
    appendRepeatedNumberParams(params, 'indicatorYearEntryId', query.indicatorYearEntryIds);
    query.valueKinds?.forEach((valueKind) => params.append('valueKind', valueKind));

    return apiGet<ObservationReadResultDto>(`${READ_API_ROOT}/observations?${params.toString()}`);
}
