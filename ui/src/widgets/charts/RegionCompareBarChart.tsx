import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue} from '../../shared/lib/format';
import type {ObservationReadResultDto} from '../../shared/types/read';

type ChartSortBy = 'regionName' | 'maxValue';
type ChartSortDirection = 'asc' | 'desc';

interface RegionCompareBarChartProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}

export function RegionCompareBarChart({result, loading, error, isDirty}: RegionCompareBarChartProps) {
    const [settingsVisible, setSettingsVisible] = useState(false);
    const [sortBy, setSortBy] = useState<ChartSortBy>('regionName');
    const [sortDirection, setSortDirection] = useState<ChartSortDirection>('asc');
    const [stackSeries, setStackSeries] = useState(false);
    const [showLabels, setShowLabels] = useState(false);
    const [maxRegions, setMaxRegions] = useState<number>(0);
    const [chartHeight, setChartHeight] = useState<number>(420);

    const preparedData = useMemo(() => {
        if (!result || result.observations.length === 0) {
            return null;
        }

        const indicatorIds = new Set(result.observations.map((item) => item.indicatorYearEntryId));
        if (indicatorIds.size !== 1) {
            return {mode: 'unsupported' as const};
        }

        const regionNames = Array.from(new Set(result.observations.map((item) => item.regionName)));
        const valueKinds = Array.from(new Set(result.observations.map((item) => item.valueKindLabel)));
        const unitCodes = Array.from(new Set(result.observations.map((item) => item.unitCode)));
        const title = result.observations[0]?.indicatorName ?? 'Сравнение регионов';

        const regionStats = regionNames.map((regionName) => {
            const matchingObservations = result.observations.filter((item) => item.regionName === regionName);
            const maxValue = matchingObservations.reduce((currentMax, item) => Math.max(currentMax, item.value), Number.NEGATIVE_INFINITY);
            return {
                regionName,
                maxValue,
                values: new Map(matchingObservations.map((item) => [item.valueKindLabel, item.value]))
            };
        });

        regionStats.sort((left, right) => {
            const directionMultiplier = sortDirection === 'asc' ? 1 : -1;
            if (sortBy === 'maxValue') {
                return (left.maxValue - right.maxValue) * directionMultiplier;
            }
            return left.regionName.localeCompare(right.regionName, 'ru') * directionMultiplier;
        });

        const limitedRegionStats = maxRegions > 0 ? regionStats.slice(0, maxRegions) : regionStats;
        const orderedRegionNames = limitedRegionStats.map((item) => item.regionName);

        const series = valueKinds.map((valueKindLabel) => ({
            name: valueKindLabel,
            type: 'bar' as const,
            stack: stackSeries ? 'total' : undefined,
            label: showLabels
                ? {
                    show: true,
                    position: 'top' as const,
                    formatter: ({value}: { value: number | string | null }) =>
                        typeof value === 'number' ? formatObservationValue(value) : ''
                }
                : undefined,
            data: limitedRegionStats.map((regionStat) => regionStat.values.get(valueKindLabel) ?? null)
        }));

        return {
            mode: 'ready' as const,
            title,
            regionNames: orderedRegionNames,
            valueKinds,
            unitSuffix: unitCodes.length === 1 ? unitCodes[0] : '',
            series,
            totalRegions: regionStats.length
        };
    }, [result, sortBy, sortDirection, stackSeries, showLabels, maxRegions]);

    return (
        <section className="panel chart-panel result-view-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>Сравнение регионов</h2>
                    <p>График для одного выбранного показателя с настройками сортировки и состава отображения.</p>
                </div>
                <div className="result-view-actions">
                    {isDirty && <div className="warning-badge">Фильтры запроса изменены</div>}
                    <button type="button" className="secondary-button"
                            onClick={() => setSettingsVisible((current) => !current)}>
                        {settingsVisible ? 'Скрыть настройки' : 'Настройки графика'}
                    </button>
                </div>
            </div>

            {settingsVisible && (
                <div className="view-settings-card">
                    <div className="view-settings-grid">
                        <label className="field">
                            <span>Сортировать регионы</span>
                            <select value={sortBy} onChange={(event) => setSortBy(event.target.value as ChartSortBy)}>
                                <option value="regionName">По названию региона</option>
                                <option value="maxValue">По максимальному значению</option>
                            </select>
                        </label>

                        <label className="field field-fit-content">
                            <span>Направление</span>
                            <select value={sortDirection}
                                    onChange={(event) => setSortDirection(event.target.value as ChartSortDirection)}>
                                <option value="asc">По возрастанию</option>
                                <option value="desc">По убыванию</option>
                            </select>
                        </label>

                        <label className="field field-fit-content">
                            <span>Макс. регионов</span>
                            <input
                                type="number"
                                min={0}
                                max={500}
                                value={maxRegions}
                                onChange={(event) => setMaxRegions(Math.max(0, Number(event.target.value) || 0))}
                            />
                        </label>

                        <label className="field field-fit-content">
                            <span>Высота графика</span>
                            <select value={chartHeight}
                                    onChange={(event) => setChartHeight(Number(event.target.value))}>
                                <option value={360}>Компактно</option>
                                <option value={420}>Стандартно</option>
                                <option value={520}>Высоко</option>
                            </select>
                        </label>
                    </div>

                    <div className="checkbox-grid chart-settings-grid">
                        <label className="check-row checkbox-card">
                            <input type="checkbox" checked={stackSeries}
                                   onChange={(event) => setStackSeries(event.target.checked)}/>
                            <span>Накопление серий</span>
                        </label>
                        <label className="check-row checkbox-card">
                            <input type="checkbox" checked={showLabels}
                                   onChange={(event) => setShowLabels(event.target.checked)}/>
                            <span>Показывать подписи значений</span>
                        </label>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="empty-state">Загрузка наблюдений…</div>
            ) : error ? (
                <div className="error-state">{error}</div>
            ) : !result ? (
                <div className="empty-state">Запрос ещё не выполнялся.</div>
            ) : preparedData?.mode === 'unsupported' ? (
                <div className="empty-state">График доступен только при выборе одного показателя.</div>
            ) : !preparedData || preparedData.regionNames.length === 0 ? (
                <div className="empty-state">По заданным параметрам нечего отображать на графике.</div>
            ) : (
                <>
                    <div className="result-view-summary-row">
                        <span className="status-badge">Регионов на графике: {preparedData.regionNames.length}</span>
                        <span className="status-badge">Всего доступно: {preparedData.totalRegions}</span>
                        <span className="status-badge">Показатель: {preparedData.title}</span>
                    </div>

                    <ReactECharts
                        style={{height: chartHeight}}
                        option={{
                            tooltip: {
                                trigger: 'axis',
                                axisPointer: {
                                    type: 'shadow'
                                },
                                valueFormatter: (value: number) => `${formatObservationValue(value)}${preparedData.unitSuffix ? ` ${preparedData.unitSuffix}` : ''}`
                            },
                            legend: {
                                type: 'scroll'
                            },
                            grid: {
                                left: 60,
                                right: 24,
                                bottom: 90,
                                top: 48
                            },
                            xAxis: {
                                type: 'category',
                                data: preparedData.regionNames,
                                axisLabel: {
                                    interval: 0,
                                    rotate: 35
                                }
                            },
                            yAxis: {
                                type: 'value',
                                name: preparedData.unitSuffix || undefined
                            },
                            series: preparedData.series
                        }}
                    />
                </>
            )}
        </section>
    );
}
