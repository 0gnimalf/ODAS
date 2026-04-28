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

export function wrapChartLabel(value: string, lineLength = 18, maxLines = 3): string {
    const normalized = value.replace(/\s+/g, ' ').trim();
    if (!normalized) return '';

    const words = normalized.split(' ');
    const lines: string[] = [];
    let currentLine = '';

    for (const word of words) {
        const candidate = currentLine ? `${currentLine} ${word}` : word;
        if (candidate.length <= lineLength) {
            currentLine = candidate;
            continue;
        }

        if (currentLine) {
            lines.push(currentLine);
            currentLine = word;
        } else {
            lines.push(word);
            currentLine = '';
        }

        if (lines.length === maxLines) break;
    }

    if (currentLine && lines.length < maxLines) {
        lines.push(currentLine);
    }

    const consumedText = lines.join(' ').replace(/…$/, '');
    const hasHiddenText = consumedText.length < normalized.length;
    if (hasHiddenText && lines.length > 0) {
        const lastIndex = lines.length - 1;
        lines[lastIndex] = truncateLabel(lines[lastIndex], Math.max(2, lineLength));
        if (!lines[lastIndex].endsWith('…')) {
            lines[lastIndex] = `${lines[lastIndex].replace(/…$/, '')}…`;
        }
    }

    return lines.join('\n');
}
