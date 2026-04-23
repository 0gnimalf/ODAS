import type {RegionReadDto} from '../types/read';

const decimalFormatter = new Intl.NumberFormat('ru-RU', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4
});

export function formatObservationValue(value: number): string {
    if (!Number.isFinite(value)) {
        return '—';
    }
    return decimalFormatter.format(value);
}

export function formatRegionLabel(region: RegionReadDto): string {
    return `${region.name} · ${region.federalDistrictShortName}`;
}
