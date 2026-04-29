import {apiPost} from './httpClient';
import {appendRepeatedNumberParams} from '../lib/queryParams';
import type {IndicatorGroupCode} from '../types/read';

const OBSERVATION_API_ROOT = '/api/observations';

export interface ObservationCollectionRequest {
    groupCode: IndicatorGroupCode;
    year: number;
    month: number;
    regionIds?: number[];
}

export interface ObservationCollectionResultDto {
    datasetCollections: number;
    receivedObservations: number;
    savedObservations: number;
    skippedObservations: number;
}

export function collectObservations(request: ObservationCollectionRequest): Promise<ObservationCollectionResultDto> {
    const params = new URLSearchParams({
        group: request.groupCode,
        year: String(request.year),
        month: String(request.month)
    });
    appendRepeatedNumberParams(params, 'regionId', request.regionIds ?? []);

    return apiPost<void, ObservationCollectionResultDto>(`${OBSERVATION_API_ROOT}/collect?${params.toString()}`);
}
