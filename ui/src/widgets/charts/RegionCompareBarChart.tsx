import {useEffect, useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue} from '../../shared/lib/format';
import type {ObservationReadDto, ObservationReadResultDto} from '../../shared/types/read';

type ChartSortBy = 'regionName' | 'maxValue';
type ChartSortDirection = 'asc' | 'desc';

interface RegionCompareBarChartProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}

interface SeriesGroupDefinition {
    key: string;
    label: string;
    unitSuffix: string;
    priority: number;
}

interface ChartSeriesDefinition {
    id: string;
    name: string;
    groupKey: string;
}

function resolveSeriesGroup(observation: ObservationReadDto): SeriesGroupDefinition {
    const rawUnitLabel = observation.unitCodeLabel?.trim() || observation.unitCode?.trim() || 'значение';
    const normalizedUnitLabel = rawUnitLabel === 'null' ? 'значение' : rawUnitLabel;
    const isPercent = observation.unitCode === 'PERCENT'
        || normalizedUnitLabel.includes('%')
        || /процент/i.test(normalizedUnitLabel);

    if (isPercent) {
        return {
            key: `percent:${normalizedUnitLabel}`,
            label: `Процентные значения (${normalizedUnitLabel})`,
            unitSuffix: normalizedUnitLabel,
            priority: 0
        };
    }

    if (observation.valueType === 'ABSOLUTE') {
        return {
            key: `absolute:${normalizedUnitLabel}`,
            label: `Абсолютные значения (${normalizedUnitLabel})`,
            unitSuffix: normalizedUnitLabel,
            priority: 1
        };
    }

    if (observation.valueType === 'RATIO') {
        return {
            key: `ratio:${normalizedUnitLabel}`,
            label: `Относительные значения (${normalizedUnitLabel})`,
            unitSuffix: normalizedUnitLabel,
            priority: 2
        };
    }

    return {
        key: `derived:${normalizedUnitLabel}`,
        label: `Производные значения (${normalizedUnitLabel})`,
        unitSuffix: normalizedUnitLabel,
        priority: 3
    };
}

export function RegionCompareBarChart({result, loading, error, isDirty}: RegionCompareBarChartProps) {
    const [settingsVisible, setSettingsVisible] = useState(false);
    const [sortBy, setSortBy] = useState<ChartSortBy>('regionName');
    const [sortDirection, setSortDirection] = useState<ChartSortDirection>('asc');
    const [stackSeries, setStackSeries] = useState(false);
    const [showLabels, setShowLabels] = useState(false);
    const [maxRegions, setMaxRegions] = useState<number>(0);
    const [chartHeight, setChartHeight] = useState<number>(420);
    const [selectedGroupKeys, setSelectedGroupKeys] = useState<string[]>([]);
    const [selectionSignature, setSelectionSignature] = useState<string>('');

    const allPreparedData = useMemo(() => {
        if (!result || result.observations.length === 0) {
            return null;
        }

        const indicatorIds = new Set(result.observations.map((item) => item.indicatorYearEntryId));
        if (indicatorIds.size !== 1) {
            return {mode: 'unsupported' as const};
        }

        const groupMap = new Map<string, SeriesGroupDefinition>();
        const seriesDefinitions = new Map<string, ChartSeriesDefinition>();

        for (const observation of result.observations) {
            const group = resolveSeriesGroup(observation);
            if (!groupMap.has(group.key)) {
                groupMap.set(group.key, group);
            }
            const seriesId = `${group.key}::${observation.valueKindLabel}`;
            if (!seriesDefinitions.has(seriesId)) {
                seriesDefinitions.set(seriesId, {
                    id: seriesId,
                    name: observation.valueKindLabel,
                    groupKey: group.key
                });
            }
        }

        const orderedGroups = Array.from(groupMap.values()).sort((left, right) => {
            if (left.priority !== right.priority) {
                return left.priority - right.priority;
            }
            return left.label.localeCompare(right.label, 'ru');
        });

        const orderedSeriesDefinitions = Array.from(seriesDefinitions.values()).sort((left, right) =>
            left.name.localeCompare(right.name, 'ru')
        );

        const regionNames = Array.from(new Set(result.observations.map((item) => item.regionName)));
        const title = result.observations[0]?.indicatorName ?? 'Сравнение регионов';

        const regionStats = regionNames.map((regionName) => {
            const matchingObservations = result.observations.filter((item) => item.regionName === regionName);
            const valueMap = new Map<string, number>();
            for (const item of matchingObservations) {
                const group = resolveSeriesGroup(item);
                valueMap.set(`${group.key}::${item.valueKindLabel}`, item.value);
            }
            const maxValue = matchingObservations.reduce(
                (currentMax, item) => Math.max(currentMax, item.value),
                Number.NEGATIVE_INFINITY
            );
            return {
                regionName,
                maxValue,
                values: valueMap
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

        return {
            mode: 'ready' as const,
            title,
            regionNames: orderedRegionNames,
            totalRegions: regionStats.length,
            groups: orderedGroups,
            seriesDefinitions: orderedSeriesDefinitions,
            regionStats: limitedRegionStats
        };
    }, [result, sortBy, sortDirection, maxRegions]);

    useEffect(() => {
        const nextSignature = allPreparedData?.mode === 'ready'
            ? allPreparedData.groups.map((group) => group.key).join('|')
            : '';

        if (!nextSignature) {
            if (selectionSignature !== '' || selectedGroupKeys.length > 0) {
                setSelectionSignature('');
                setSelectedGroupKeys([]);
            }
            return;
        }

        if (nextSignature !== selectionSignature && allPreparedData?.mode === 'ready') {
            setSelectionSignature(nextSignature);
            setSelectedGroupKeys(allPreparedData.groups.map((group) => group.key));
        }
    }, [allPreparedData, selectionSignature, selectedGroupKeys.length]);

    const visibleCharts = useMemo(() => {
        if (!allPreparedData || allPreparedData.mode !== 'ready') {
            return [];
        }

        const selectedGroupKeySet = new Set(selectedGroupKeys);
        return allPreparedData.groups
            .filter((group) => selectedGroupKeySet.has(group.key))
            .map((group) => {
                const seriesDefinitions = allPreparedData.seriesDefinitions.filter((item) => item.groupKey === group.key);
                return {
                    group,
                    series: seriesDefinitions.map((seriesDefinition) => ({
                        name: seriesDefinition.name,
                        type: 'bar' as const,
                        stack: stackSeries ? group.key : undefined,
                        label: showLabels
                            ? {
                                show: true,
                                position: 'top' as const,
                                formatter: ({value}: { value: number | string | null }) =>
                                    typeof value === 'number' ? formatObservationValue(value) : ''
                            }
                            : undefined,
                        data: allPreparedData.regionStats.map((regionStat) => regionStat.values.get(seriesDefinition.id) ?? null)
                    }))
                };
            })
            .filter((chart) => chart.series.length > 0);
    }, [allPreparedData, selectedGroupKeys, showLabels, stackSeries]);

    const toggleGroup = (groupKey: string) => {
        setSelectedGroupKeys((current) => {
            if (current.includes(groupKey)) {
                return current.filter((item) => item !== groupKey);
            }
            return [...current, groupKey];
        });
    };

    return (
        <section className="panel chart-panel result-view-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>Сравнение регионов</h2>
                    <p>Серии разбиваются по типам столбцов, чтобы процентные и абсолютные значения не смешивались на
                        одной шкале.</p>
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
                            <span>Накопление серий внутри типа</span>
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
            ) : allPreparedData?.mode === 'unsupported' ? (
                <div className="empty-state">График доступен только при выборе одного показателя.</div>
            ) : !allPreparedData || allPreparedData.regionNames.length === 0 ? (
                <div className="empty-state">По заданным параметрам нечего отображать на графике.</div>
            ) : (
                <>
                    <div className="result-view-summary-row">
                        <span className="status-badge">Регионов на графике: {allPreparedData.regionNames.length}</span>
                        <span className="status-badge">Всего доступно: {allPreparedData.totalRegions}</span>
                        <span className="status-badge">Показатель: {allPreparedData.title}</span>
                        <span className="status-badge">Типов серий: {allPreparedData.groups.length}</span>
                    </div>

                    <div className="view-settings-card chart-group-selector-card">
                        <div className="selector-header-row compact-bottom-gap">
                            <strong>Типы столбцов на графике</strong>
                            <div className="inline-actions wrap">
                                <button
                                    type="button"
                                    onClick={() => setSelectedGroupKeys(allPreparedData.groups.map((group) => group.key))}
                                >
                                    Выбрать все
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setSelectedGroupKeys([])}
                                >
                                    Снять выбор
                                </button>
                            </div>
                        </div>
                        <p className="muted-text compact-bottom-gap">
                            Можно выбрать несколько типов. Каждый тип будет показан на отдельном графике со своей
                            шкалой.
                        </p>
                        <div className="checkbox-grid">
                            {allPreparedData.groups.map((group) => (
                                <label key={group.key} className="check-row checkbox-card">
                                    <input
                                        type="checkbox"
                                        checked={selectedGroupKeys.includes(group.key)}
                                        onChange={() => toggleGroup(group.key)}
                                    />
                                    <span>{group.label}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {visibleCharts.length === 0 ? (
                        <div className="empty-state compact">Выберите хотя бы один тип столбцов для отображения
                            графика.</div>
                    ) : (
                        <div className="chart-group-stack">
                            {visibleCharts.map((chart) => (
                                <div key={chart.group.key} className="chart-group-panel">
                                    <div className="result-view-summary-row compact-bottom-gap">
                                        <span className="status-badge">{chart.group.label}</span>
                                        <span className="status-badge">Серий: {chart.series.length}</span>
                                    </div>
                                    <ReactECharts
                                        style={{height: chartHeight}}
                                        option={{
                                            tooltip: {
                                                trigger: 'axis',
                                                axisPointer: {
                                                    type: 'shadow'
                                                },
                                                valueFormatter: (value: number) => `${formatObservationValue(value)}${chart.group.unitSuffix ? ` ${chart.group.unitSuffix}` : ''}`
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
                                                data: allPreparedData.regionNames,
                                                axisLabel: {
                                                    interval: 0,
                                                    rotate: 35
                                                }
                                            },
                                            yAxis: {
                                                type: 'value',
                                                name: chart.group.unitSuffix || undefined
                                            },
                                            series: chart.series
                                        }}
                                    />
                                </div>
                            ))}
                        </div>
                    )}
                </>
            )}
        </section>
    );
}
