import type {RegionReadDto} from '../types/read';

const decimalFormatter = new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 0, maximumFractionDigits: 4});
const percentFormatter = new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 0, maximumFractionDigits: 2});

export const CHART_COLOR_PALETTE = [
    '#2563eb',
    '#16a34a',
    '#f97316',
    '#9333ea',
    '#0891b2',
    '#dc2626',
    '#65a30d',
    '#7c3aed',
    '#0d9488',
    '#ca8a04',
    '#db2777',
    '#475569',
    '#0284c7',
    '#ea580c'
];

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
    return wrapPlainText(value, lineLength, maxLines, true).join('\n');
}

export function wrapTooltipText(value: string | number | null | undefined, lineLength = 48, maxLines = 2): string {
    return wrapPlainText(String(value ?? '—'), lineLength, maxLines, true)
        .map(escapeHtml)
        .join('<br/>');
}

export function buildTooltipHtml(title: string, rows: Array<[string, string | number | null | undefined]> = []): string {
    const rowHtml = rows.map(([label, value]) => `
        <div style="display:grid;grid-template-columns:4fr 1.5fr;gap:10px;align-items:start;margin-top:6px;">
            <span style="color:#64748b;">${escapeHtml(label)}</span>
            <strong style="min-width:0;font-weight:600;color:#0f172a;">${wrapTooltipText(value, 44, 5)}</strong>
            
        </div>
    `).join('');

    return `
        <div style="max-width:560px;white-space:normal;overflow-wrap:anywhere;word-break:normal;line-height:1.35;">
            <div style="font-weight:700;color:#0f172a;margin-bottom:4px;">${wrapTooltipText(title, 44, 5)}</div>
            ${rowHtml}
        </div>
    `;
}

export function buildContainedTooltip<T extends Record<string, unknown>>(tooltip: T): T & {
    confine: boolean;
    appendToBody: boolean;
    extraCssText: string;
    position: typeof clampTooltipPosition;
} {
    return {
        confine: true,
        appendToBody: true,
        extraCssText: [
            'max-width:680px',
            'white-space:normal',
            'overflow-wrap:anywhere',
            'word-break:normal',
            'box-shadow:0 18px 38px rgba(15,23,42,.2)',
            'border-radius:12px',
            'padding:10px 12px'
        ].join(';'),
        position: clampTooltipPosition,
        ...tooltip
    };
}

function clampTooltipPosition(
    point: number[],
    _params: unknown,
    _dom: unknown,
    _rect: unknown,
    size: { contentSize: number[]; viewSize: number[] }
): [number, number] {
    const padding = 12;
    const [contentWidth, contentHeight] = size.contentSize;
    const [viewWidth, viewHeight] = size.viewSize;
    const maxX = Math.max(padding, viewWidth - contentWidth - padding);
    const maxY = Math.max(padding, viewHeight - contentHeight - padding);
    const x = Math.min(Math.max(point[0] + padding, padding), maxX);
    const y = Math.min(Math.max(point[1] + padding, padding), maxY);
    return [x, y];
}

function wrapPlainText(value: string, lineLength: number, maxLines: number, ellipsis: boolean): string[] {
    const normalized = value.replace(/\s+/g, ' ').trim();
    if (!normalized) return [''];

    const words = normalized.split(' ').flatMap((word) => splitLongWord(word, lineLength));
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

    const consumedLength = lines.join(' ').replace(/…$/, '').length;
    const hasHiddenText = consumedLength < normalized.length;
    if (ellipsis && hasHiddenText && lines.length > 0) {
        const lastIndex = lines.length - 1;
        lines[lastIndex] = truncateLabel(lines[lastIndex], Math.max(2, lineLength));
        if (!lines[lastIndex].endsWith('…')) {
            lines[lastIndex] = `${lines[lastIndex].replace(/…$/, '')}…`;
        }
    }

    return lines.length > 0 ? lines : [normalized];
}

function splitLongWord(word: string, lineLength: number): string[] {
    if (word.length <= lineLength) return [word];

    const chunks: string[] = [];
    for (let index = 0; index < word.length; index += lineLength) {
        chunks.push(word.slice(index, index + lineLength));
    }
    return chunks;
}

function escapeHtml(value: string): string {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
