import type {RegionReadDto} from '../types/read';

const decimalFormatter = new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 0, maximumFractionDigits: 4});
const percentFormatter = new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 0, maximumFractionDigits: 2});

export const MONTH_LABELS: Record<number, string> = {
    1: 'Январь', 2: 'Февраль', 3: 'Март', 4: 'Апрель', 5: 'Май', 6: 'Июнь',
    7: 'Июль', 8: 'Август', 9: 'Сентябрь', 10: 'Октябрь', 11: 'Ноябрь', 12: 'Декабрь'
};

export function formatObservationValue(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) return '—';
    return decimalFormatter.format(value);
}

export function formatPercentValue(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) return '—';
    return `${percentFormatter.format(value)} %`;
}

export function formatRegionLabel(region: RegionReadDto): string {
    return `${region.name} · ${region.federalDistrictShortName}`;
}

export function truncateLabel(value: string, maxLength = 44): string {
    const normalized = value.trim();
    if (normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.slice(0, Math.max(0, maxLength - 1)).trimEnd()}…`;
}
