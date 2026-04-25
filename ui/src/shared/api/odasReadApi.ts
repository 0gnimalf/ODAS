import {API_BASE_URL} from '../config/env';
import {appendRepeatedNumberParams} from '../lib/queryParams';
import type {
    IndicatorGroupCode,
    IndicatorGroupReadDto,
    IndicatorTreeNodeReadDto,
    ObservationQuery,
    ObservationReadResultDto,
    RegionReadDto
} from '../types/read';

async function fetchJson<T>(path: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`);
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
    }
    return await response.json() as Promise<T>;
}

async function fetchVoid(path: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}${path}`);
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
    }
}

export function getIndicatorGroups(): Promise<IndicatorGroupReadDto[]> {
    return fetchJson<IndicatorGroupReadDto[]>('/internal/temp/read/groups');
}

export function getRegions(): Promise<RegionReadDto[]> {
    return fetchJson<RegionReadDto[]>('/internal/temp/read/regions');
}

export function getIndicatorTree(
    groupCode: IndicatorGroupCode,
    year: number
): Promise<IndicatorTreeNodeReadDto[]> {
    const params = new URLSearchParams({
        group: groupCode,
        year: String(year)
    });
    return fetchJson<IndicatorTreeNodeReadDto[]>(`/internal/temp/read/indicators/tree?${params.toString()}`);
}

export function requestIndicatorTreeSync(groupCode: IndicatorGroupCode, year: number): Promise<void> {
    const params = new URLSearchParams({
        group: groupCode,
        year: String(year)
    });
    return fetchVoid(`/internal/temp/reference/indicators?${params.toString()}`);
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

    return fetchJson<ObservationReadResultDto>(`/internal/temp/read/observations?${params.toString()}`);
}
