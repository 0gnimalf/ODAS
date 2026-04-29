import {apiPost, apiPostVoid} from './httpClient';
import type {IndicatorGroupCode} from '../types/read';

const REFERENCE_API_ROOT = '/api/reference';

export interface ReferenceSyncResultDto {
    received: number;
    created: number;
    updated: number;
    skipped: number;
}

export function requestRegionSync(force = false): Promise<ReferenceSyncResultDto> {
    const params = new URLSearchParams({force: String(force)});
    return apiPost<void, ReferenceSyncResultDto>(`${REFERENCE_API_ROOT}/regions/sync?${params.toString()}`);
}

export function requestIndicatorTreeSync(groupCode: IndicatorGroupCode, year: number): Promise<void> {
    const params = new URLSearchParams({
        group: groupCode,
        year: String(year)
    });
    return apiPostVoid(`${REFERENCE_API_ROOT}/indicators/sync?${params.toString()}`);
}
