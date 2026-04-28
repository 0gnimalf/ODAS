import type {IndicatorTreeNodeReadDto, ObservationReadResultDto} from '../types/read';

export type PopulationByRegion = Record<number, number>;

export function findPopulationIndicatorEntryId(nodes: IndicatorTreeNodeReadDto[]): number | null {
    const stack = [...nodes];
    while (stack.length > 0) {
        const node = stack.shift()!;
        const normalized = normalizeText(node.name);
        if (normalized.includes('численность населения') || normalized.includes('население')) {
            return node.id;
        }
        stack.push(...node.children);
    }
    return null;
}

export function buildPopulationByRegion(result: ObservationReadResultDto | null): PopulationByRegion {
    const values: PopulationByRegion = {};
    for (const observation of result?.observations ?? []) {
        if (observation.valueKind === 'POPULATION' && Number.isFinite(observation.value) && observation.value > 0) {
            values[observation.regionId] = observation.value;
        }
    }
    return values;
}

export function countKnownPopulation(populationByRegion: PopulationByRegion, regionIds: number[]): number {
    return regionIds.filter((regionId) => Number.isFinite(populationByRegion[regionId]) && populationByRegion[regionId] > 0).length;
}

function normalizeText(value: string): string {
    return value.trim().toLowerCase().replace(/ё/g, 'е').replace(/\s+/g, ' ');
}
