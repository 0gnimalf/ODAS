import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {
    buildContainedTooltip,
    buildTooltipHtml,
    CHART_COLOR_PALETTE,
    formatObservationValue,
    truncateLabel,
    wrapChartLabel
} from '../../shared/lib/format';
import type {ObservationReadDto, ObservationReadResultDto} from '../../shared/types/read';

interface RegionCompareBarChartProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}

type SortBy = 'regionName' | 'totalValue';
type SortDirection = 'asc' | 'desc';

interface ChartGroup {
    key: string;
    label: string;
    unitSuffix: string;
    priority: number;
    observations: ObservationReadDto[];
}

export function RegionCompareBarChart({result, loading, error, isDirty}: RegionCompareBarChartProps) {
    const [showSettings, setShowSettings] = useState(false);
    const [sortBy, setSortBy] = useState<SortBy>('totalValue');
    const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
    const [maxRegions, setMaxRegions] = useState(25);
    const [showLabels, setShowLabels] = useState(false);

    const groups = useMemo(
        () => buildChartGroups(result?.observations ?? [], sortBy, sortDirection, maxRegions),
        [result, sortBy, sortDirection, maxRegions]
    );

    if (loading) {
        return <section className="panel chart-panel result-view-panel">
            <div className="empty-state">Загрузка графика…</div>
        </section>;
    }
    if (error) {
        return <section className="panel chart-panel result-view-panel">
            <div className="error-state">{error}</div>
        </section>;
    }

    return (
        <section className="panel chart-panel result-view-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>Сравнение регионов</h2>
                    <p>Показатели разнесены по совместимым шкалам.</p>
                </div>
                <div className="result-view-actions">
                    {isDirty && <span className="warning-badge">Фильтры запроса изменены</span>}
                    <button type="button" className="secondary-button"
                            onClick={() => setShowSettings((current) => !current)}>
                        {showSettings ? 'Скрыть настройки' : 'Настройки графика'}
                    </button>
                </div>
            </div>

            {showSettings && (
                <div className="view-settings-card">
                    <div className="view-settings-grid">
                        <label className="field">
                            <span>Сортировка</span>
                            <select value={sortBy} onChange={(event) => setSortBy(event.target.value as SortBy)}>
                                <option value="totalValue">По сумме значений</option>
                                <option value="regionName">По названию региона</option>
                            </select>
                        </label>
                        <label className="field field-fit-content">
                            <span>Направление</span>
                            <select value={sortDirection}
                                    onChange={(event) => setSortDirection(event.target.value as SortDirection)}>
                                <option value="desc">По убыванию</option>
                                <option value="asc">По возрастанию</option>
                            </select>
                        </label>
                        <label className="field field-fit-content">
                            <span>Видимых регионов</span>
                            <input type="number" min={5} max={120} value={maxRegions}
                                   onChange={(event) => setMaxRegions(clamp(Number(event.target.value) || 5, 5, 120))}/>
                        </label>
                        <label className="check-row checkbox-card compact-checkbox-card">
                            <input type="checkbox" checked={showLabels}
                                   onChange={(event) => setShowLabels(event.target.checked)}/>
                            <span>Подписи значений</span>
                        </label>
                    </div>
                </div>
            )}

            {groups.length === 0 && <div className="empty-state">Нет наблюдений для построения графика.</div>}

            <div className="chart-block-stack">
                {groups.map((group) => (
                    <article key={group.key} className="chart-subpanel">
                        <div className="chart-subpanel-header">
                            <h3>{group.label}</h3>
                            <span>{group.observations.length} наблюдений</span>
                        </div>
                        <ReactECharts
                            style={{height: Math.max(340, Math.min(760, group.regionNames.length * 34 + 120))}}
                            option={buildOption(group, showLabels)}
                            notMerge
                        />
                    </article>
                ))}
            </div>
        </section>
    );
}

function buildOption(group: ReturnType<typeof buildChartGroups>[number], showLabels: boolean) {
    const seriesNames = group.series.map((series) => series.name);
    return {
        color: CHART_COLOR_PALETTE,
        tooltip: buildContainedTooltip({
            trigger: 'axis',
            axisPointer: {type: 'shadow'},
            formatter: (params: Array<{ seriesName: string; value: number | null; axisValue?: string }>) => {
                const regionName = params[0]?.axisValue ?? '';
                return buildTooltipHtml(regionName, params.map((item) => [
                    item.seriesName,
                    item.value == null ? '—' : `${formatObservationValue(item.value)} ${group.unitSuffix}`
                ]));
            }
        }),
        legend: {
            top: 0,
            type: 'scroll',
            data: seriesNames,
            formatter: (value: string) => wrapChartLabel(value, 26, 2),
            textStyle: {lineHeight: 15}
        },
        grid: {left: 280, right: showLabels ? 150 : 42, top: 78, bottom: 36, containLabel: false},
        xAxis: {type: 'value', axisLabel: {formatter: (value: number) => formatObservationValue(value)}},
        yAxis: {
            type: 'category',
            inverse: true,
            data: group.regionNames,
            axisLabel: {
                interval: 0,
                width: 250,
                lineHeight: 15,
                overflow: 'break',
                formatter: (value: string) => wrapChartLabel(value, 25, 3)
            }
        },
        dataZoom: group.regionNames.length > 18 ? [
            {
                type: 'slider',
                yAxisIndex: 0,
                right: 4,
                width: 12,
                start: 0,
                end: Math.min(100, 18 / group.regionNames.length * 100)
            },
            {type: 'inside', yAxisIndex: 0}
        ] : [],
        series: group.series.map((series) => ({
            name: series.name,
            type: 'bar',
            barMaxWidth: 18,
            label: showLabels ? {
                show: true,
                position: 'right',
                formatter: ({value}: { value: number | null }) => value == null ? '' : formatObservationValue(value)
            } : undefined,
            data: series.values
        }))
    };
}

function buildChartGroups(observations: ObservationReadDto[], sortBy: SortBy, sortDirection: SortDirection, maxRegions: number) {
    if (observations.length === 0) return [];

    const groupBuckets = new Map<string, ChartGroup>();
    for (const observation of observations) {
        const meta = resolveGroupMeta(observation);
        const current = groupBuckets.get(meta.key) ?? {...meta, observations: []};
        current.observations.push(observation);
        groupBuckets.set(meta.key, current);
    }

    return Array.from(groupBuckets.values())
        .sort((left, right) => left.priority - right.priority || left.label.localeCompare(right.label, 'ru'))
        .map((group) => {
            const regionNames = Array.from(new Set(group.observations.map((item) => item.regionName)));
            const seriesNames = Array.from(new Set(group.observations.map((item) => `${item.indicatorName} · ${item.valueKindLabel}`)));
            const valuesByRegionAndSeries = new Map<string, number>();
            for (const observation of group.observations) {
                valuesByRegionAndSeries.set(makeKey(observation.regionName, `${observation.indicatorName} · ${observation.valueKindLabel}`), observation.value);
            }

            const regionRows = regionNames.map((regionName) => ({
                regionName,
                totalValue: seriesNames.reduce((sum, seriesName) => sum + Math.abs(valuesByRegionAndSeries.get(makeKey(regionName, seriesName)) ?? 0), 0)
            }));
            regionRows.sort((left, right) => {
                const direction = sortDirection === 'asc' ? 1 : -1;
                if (sortBy === 'regionName') return left.regionName.localeCompare(right.regionName, 'ru') * direction;
                return (left.totalValue - right.totalValue) * direction;
            });

            const limitedRegions = regionRows.slice(0, maxRegions).map((item) => item.regionName);
            return {
                ...group,
                regionNames: limitedRegions,
                series: seriesNames.map((seriesName) => ({
                    name: truncateLabel(seriesName, 86),
                    values: limitedRegions.map((regionName) => valuesByRegionAndSeries.get(makeKey(regionName, seriesName)) ?? null)
                }))
            };
        });
}

function resolveGroupMeta(observation: ObservationReadDto) {
    const unit = observation.unitCodeLabel || observation.unitCode || 'значение';
    const isPercent = observation.unitCode === 'PERCENT' || unit.includes('%') || /процент/i.test(unit);
    if (isPercent) {
        return {key: `percent:${unit}`, label: `Процентные значения · ${unit}`, unitSuffix: unit, priority: 1};
    }
    if (observation.valueType === 'RATIO') {
        return {key: `ratio:${unit}`, label: `Относительные значения · ${unit}`, unitSuffix: unit, priority: 2};
    }
    return {key: `absolute:${unit}`, label: `Абсолютные значения · ${unit}`, unitSuffix: unit, priority: 0};
}

function makeKey(regionName: string, seriesName: string) {
    return `${regionName}::${seriesName}`;
}

function clamp(value: number, min: number, max: number) {
    return Math.min(max, Math.max(min, value));
}
