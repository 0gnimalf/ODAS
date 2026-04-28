import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, formatPercentValue, truncateLabel} from '../../shared/lib/format';
import type {PopulationByRegion} from '../../shared/lib/population';
import {countKnownPopulation} from '../../shared/lib/population';
import type {RegionComparisonItemDto, RegionComparisonResultDto} from '../../shared/types/analysis';

export function ComparisonResultPanel({result, loading, error, isDirty, populationByRegion, populationWarning}: {
    result: RegionComparisonResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
    populationByRegion: PopulationByRegion;
    populationWarning: string | null;
}) {
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
    const [maxRegions, setMaxRegions] = useState(25);
    const [hideMissing, setHideMissing] = useState(true);
    const [filter, setFilter] = useState('');

    const items = useMemo(() => {
        const normalizedFilter = filter.trim().toLowerCase();
        const next = (result?.items ?? [])
            .filter((item) => (!hideMissing || !item.missing) && (!normalizedFilter || item.regionName.toLowerCase().includes(normalizedFilter)));
        next.sort((a, b) => {
            const left = a.value ?? Number.NEGATIVE_INFINITY;
            const right = b.value ?? Number.NEGATIVE_INFINITY;
            return sortDirection === 'desc' ? right - left : left - right;
        });
        return next.slice(0, maxRegions);
    }, [result, hideMissing, filter, sortDirection, maxRegions]);

    const perCapitaItems = useMemo(
        () => items.map((item) => ({
            ...item,
            population: populationByRegion[item.regionId] ?? null,
            perCapitaValue: item.value != null && populationByRegion[item.regionId] > 0
                ? item.value / populationByRegion[item.regionId]
                : null
        })),
        [items, populationByRegion]
    );

    const perCapitaSummary = useMemo(() => buildPerCapitaSummary(perCapitaItems), [perCapitaItems]);
    const knownPopulation = result ? countKnownPopulation(populationByRegion, result.items.map((item) => item.regionId)) : 0;

    return (
        <div className="results-stack">
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Сравнение регионов</h2>
                        <p>Абсолютное значение и доля показаны в одном рейтинге. Отдельно строится пересчёт на
                            численность населения.</p>
                    </div>
                    <div className="status-badges">
                        {result && <span className="status-badge">{result.valueKindLabel}</span>}
                        {isDirty && <span className="warning-badge">Параметры сценария изменены</span>}
                    </div>
                </div>

                <div className="view-settings-grid analytics-inline-settings-grid">
                    <label className="field field-fit-content">
                        <span>Порядок рейтинга</span>
                        <select value={sortDirection}
                                onChange={(event) => setSortDirection(event.target.value as 'asc' | 'desc')}>
                            <option value="desc">По убыванию</option>
                            <option value="asc">По возрастанию</option>
                        </select>
                    </label>
                    <label className="field field-fit-content">
                        <span>Макс. регионов</span>
                        <input type="number" min={5} max={120} value={maxRegions}
                               onChange={(event) => setMaxRegions(clamp(Number(event.target.value) || 25, 5, 120))}/>
                    </label>
                    <label className="field">
                        <span>Фильтр по региону</span>
                        <input type="search" value={filter} onChange={(event) => setFilter(event.target.value)}
                               placeholder="Название региона"/>
                    </label>
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input type="checkbox" checked={hideMissing}
                               onChange={(event) => setHideMissing(event.target.checked)}/>
                        <span>Скрывать missing</span>
                    </label>
                </div>
            </section>

            <section className="panel chart-panel result-view-panel">
                <Header title="Абсолютные значения и доли"
                        description="Подпись у каждого столбца содержит значение и долю региона в общем объёме."/>
                <Guard loading={loading} error={error} hasData={items.length > 0}
                       emptyMessage="Нет регионов для отображения рейтинга.">
                    <ReactECharts style={{height: chartHeight(items.length)}}
                                  option={buildCombinedRankingOption(items, result?.unitCodeLabel ?? '')} notMerge/>
                </Guard>
            </section>

            <section className="panel chart-panel result-view-panel">
                <Header title="Нормализация по численности населения"
                        description="Значение показателя делится на численность населения соответствующего региона за выбранный год."/>
                {populationWarning && <div className="warning-note top-margin-8">{populationWarning}</div>}
                {!populationWarning && result && knownPopulation < result.items.length && (
                    <div className="warning-note top-margin-8">Население найдено не для всех
                        регионов: {knownPopulation} из {result.items.length}.</div>
                )}
                <Guard loading={loading} error={error}
                       hasData={perCapitaItems.some((item) => item.perCapitaValue != null)}
                       emptyMessage="Нет данных для нормализации по населению.">
                    <ReactECharts style={{height: chartHeight(perCapitaItems.length)}}
                                  option={buildPerCapitaOption(perCapitaItems, result?.unitCodeLabel ?? '')} notMerge/>
                </Guard>
                <div className="analytics-card-grid metrics-grid-compact top-margin-16">
                    <Card title="Регионов с населением" value={`${knownPopulation} / ${result?.items.length ?? 0}`}/>
                    <Card title="Среднее на человека" value={formatObservationValue(perCapitaSummary.average)}/>
                    <Card title="Максимум на человека" value={formatObservationValue(perCapitaSummary.max)}/>
                    <Card title="Минимум на человека" value={formatObservationValue(perCapitaSummary.min)}/>
                </div>
            </section>

            <section className="panel result-view-panel">
                <Header title="Сводка распределения"
                        description="Основные статистики по исходным значениям выбранного показателя."/>
                <Guard loading={loading} error={error} hasData={Boolean(result)}
                       emptyMessage="Сводка ещё не сформирована.">
                    <div className="analytics-card-grid metrics-grid-compact">
                        <Card title="Запрошено регионов" value={result?.summary.requestedRegionCount ?? 0}/>
                        <Card title="Найдено значений" value={result?.summary.foundRegionCount ?? 0}/>
                        <Card title="Минимум" value={formatObservationValue(result?.summary.minValue)}/>
                        <Card title="Максимум" value={formatObservationValue(result?.summary.maxValue)}/>
                        <Card title="Среднее" value={formatObservationValue(result?.summary.averageValue)}/>
                        <Card title="Медиана" value={formatObservationValue(result?.summary.medianValue)}/>
                        <Card title="Итого" value={formatObservationValue(result?.summary.totalValue)}/>
                    </div>
                </Guard>
            </section>

            <section className="panel table-panel result-view-panel">
                <Header title="Таблица сравнения" description="Регион, значение, доля, ранг и пересчёт на население."/>
                <Guard loading={loading} error={error} hasData={items.length > 0}
                       emptyMessage="Нет строк для таблицы сравнения.">
                    <div className="table-wrapper table-wrapper-scroll comparison-table-scroll">
                        <table>
                            <thead>
                            <tr>
                                <th>Ранг</th>
                                <th>Регион</th>
                                <th>Значение</th>
                                <th>Доля</th>
                                <th>Население</th>
                                <th>На человека</th>
                                <th>Δ к среднему</th>
                            </tr>
                            </thead>
                            <tbody>
                            {perCapitaItems.map((item) => (
                                <tr key={item.regionId}>
                                    <td>{item.rank ?? '—'}</td>
                                    <td>{item.regionName}</td>
                                    <td>{formatObservationValue(item.value)}</td>
                                    <td>{formatPercentValue(item.shareOfTotalPercent)}</td>
                                    <td>{formatObservationValue(item.population)}</td>
                                    <td>{formatObservationValue(item.perCapitaValue)}</td>
                                    <td>{formatObservationValue(item.deltaFromAverage)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </Guard>
            </section>
        </div>
    );
}

function buildCombinedRankingOption(items: RegionComparisonItemDto[], unitLabel: string) {
    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: {type: 'shadow'},
            formatter: (params: Array<{ dataIndex: number; value: number }>) => {
                const item = items[params[0]?.dataIndex ?? 0];
                return `${item.regionName}<br/>Значение: ${formatObservationValue(item.value)} ${unitLabel}<br/>Доля: ${formatPercentValue(item.shareOfTotalPercent)}`;
            }
        },
        grid: {left: 230, right: 160, top: 24, bottom: 34},
        xAxis: {type: 'value', axisLabel: {formatter: (value: number) => formatObservationValue(value)}},
        yAxis: {
            type: 'category',
            inverse: true,
            data: items.map((item) => item.regionName),
            axisLabel: {width: 205, overflow: 'truncate'}
        },
        dataZoom: items.length > 18 ? [
            {type: 'slider', yAxisIndex: 0, right: 4, width: 12, start: 0, end: Math.min(100, 18 / items.length * 100)},
            {type: 'inside', yAxisIndex: 0}
        ] : [],
        series: [{
            type: 'bar',
            barMaxWidth: 20,
            label: {
                show: true,
                position: 'right',
                formatter: ({dataIndex}: { dataIndex: number }) => {
                    const item = items[dataIndex];
                    return `${formatObservationValue(item.value)} (${formatPercentValue(item.shareOfTotalPercent)})`;
                }
            },
            data: items.map((item) => item.value ?? 0)
        }]
    };
}

function buildPerCapitaOption(items: Array<RegionComparisonItemDto & {
    population: number | null;
    perCapitaValue: number | null
}>, unitLabel: string) {
    const sorted = [...items].sort((left, right) => (right.perCapitaValue ?? Number.NEGATIVE_INFINITY) - (left.perCapitaValue ?? Number.NEGATIVE_INFINITY));
    return {
        tooltip: {
            trigger: 'axis',
            axisPointer: {type: 'shadow'},
            formatter: (params: Array<{ dataIndex: number }>) => {
                const item = sorted[params[0]?.dataIndex ?? 0];
                return `${item.regionName}<br/>На человека: ${formatObservationValue(item.perCapitaValue)} ${unitLabel}/чел.<br/>Население: ${formatObservationValue(item.population)}`;
            }
        },
        grid: {left: 230, right: 120, top: 24, bottom: 34},
        xAxis: {type: 'value', axisLabel: {formatter: (value: number) => formatObservationValue(value)}},
        yAxis: {
            type: 'category',
            inverse: true,
            data: sorted.map((item) => truncateLabel(item.regionName, 42)),
            axisLabel: {width: 205, overflow: 'truncate'}
        },
        dataZoom: sorted.length > 18 ? [
            {
                type: 'slider',
                yAxisIndex: 0,
                right: 4,
                width: 12,
                start: 0,
                end: Math.min(100, 18 / sorted.length * 100)
            },
            {type: 'inside', yAxisIndex: 0}
        ] : [],
        series: [{
            type: 'bar',
            barMaxWidth: 20,
            label: {
                show: true,
                position: 'right',
                formatter: ({value}: { value: number | null }) => formatObservationValue(value)
            },
            data: sorted.map((item) => item.perCapitaValue)
        }]
    };
}

function buildPerCapitaSummary(items: Array<{ perCapitaValue: number | null }>) {
    const values = items.map((item) => item.perCapitaValue).filter((value): value is number => value != null && Number.isFinite(value));
    if (values.length === 0) return {average: null, min: null, max: null};
    return {
        average: values.reduce((sum, value) => sum + value, 0) / values.length,
        min: Math.min(...values),
        max: Math.max(...values)
    };
}

function chartHeight(rowCount: number) {
    return Math.max(340, Math.min(820, rowCount * 34 + 90));
}

function Header({title, description}: { title: string; description: string }) {
    return <div className="panel-header align-start compact-gap">
        <div><h2>{title}</h2><p>{description}</p></div>
    </div>;
}

function Guard({loading, error, hasData, emptyMessage, children}: {
    loading: boolean;
    error: string | null;
    hasData: boolean;
    emptyMessage: string;
    children: ReactNode;
}) {
    if (loading) return <div className="empty-state">Загрузка аналитических данных…</div>;
    if (error) return <div className="error-state">{error}</div>;
    if (!hasData) return <div className="empty-state">{emptyMessage}</div>;
    return <>{children}</>;
}

function Card({title, value}: { title: string; value: ReactNode }) {
    return <article className="analytics-kpi-card"><span className="analytics-kpi-title">{title}</span><strong
        className="analytics-kpi-value">{value}</strong></article>;
}

function clamp(value: number, min: number, max: number) {
    return Math.min(max, Math.max(min, value));
}
