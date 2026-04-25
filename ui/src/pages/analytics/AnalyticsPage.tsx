import {useEffect, useMemo, useState} from 'react';
import {getIndicatorGroups, getIndicatorTree, getRegions, requestIndicatorTreeSync} from '../../shared/api/odasReadApi';
import {
    buildMonthlySeries,
    buildRegionIndicatorMatrix,
    buildSubtreeSlice,
    calculatePeriodGrowthMetrics,
    compareRegions
} from '../../shared/api/odasAnalysisApi';
import {formatRegionLabel, MONTH_LABELS} from '../../shared/lib/format';
import {expandSelectedIdsWithDescendants} from '../../shared/lib/tree';
import type {
    IndicatorGroupCode,
    IndicatorGroupReadDto,
    IndicatorTreeNodeReadDto,
    RegionReadDto
} from '../../shared/types/read';
import type {
    MonthlySeriesResultDto,
    ObservationValueKind,
    PeriodGrowthMetricsResultDto,
    RegionComparisonResultDto,
    RegionIndicatorMatrixResultDto,
    SubtreeSliceResultDto
} from '../../shared/types/analysis';
import {OBSERVATION_VALUE_KIND_OPTIONS} from '../../shared/types/analysis';
import {IndicatorTreePanel} from '../../widgets/indicator-tree/IndicatorTreePanel';
import {RegionMultiSelect} from '../../widgets/filter-panel/RegionMultiSelect';
import {SeriesResultPanel} from '../../widgets/analytics/SeriesResultPanel';
import {ComparisonResultPanel} from '../../widgets/analytics/ComparisonResultPanel';
import {SubtreeResultPanel} from '../../widgets/analytics/SubtreeResultPanel';
import {MatrixResultPanel} from '../../widgets/analytics/MatrixResultPanel';

const CURRENT_YEAR = new Date().getFullYear();

type Scenario = 'series' | 'compare' | 'subtree' | 'matrix';

const SCENARIOS: Array<{ key: Scenario; title: string; description: string }> = [
    {key: 'series', title: 'Ряд и темпы', description: 'Помесячный ряд, чистые значения, кварталы и темпы роста.'},
    {key: 'compare', title: 'Сравнение регионов', description: 'Рейтинг и статистика по одному показателю.'},
    {key: 'subtree', title: 'Поддерево', description: 'Срез показателей внутри выбранного корня.'},
    {key: 'matrix', title: 'Матрица', description: 'Сравнение регионов и набора показателей.'}
];

export function AnalyticsPage() {
    const [groups, setGroups] = useState<IndicatorGroupReadDto[]>([]);
    const [regions, setRegions] = useState<RegionReadDto[]>([]);
    const [tree, setTree] = useState<IndicatorTreeNodeReadDto[]>([]);

    const [scenario, setScenario] = useState<Scenario>('series');
    const [groupCode, setGroupCode] = useState<IndicatorGroupCode | ''>('');
    const [year, setYear] = useState(CURRENT_YEAR);
    const [month, setMonth] = useState(1);
    const [valueKind, setValueKind] = useState<ObservationValueKind>('ACTUAL_CONSOLIDATED_SUBJECT_BUDGET');

    const [bootLoading, setBootLoading] = useState(true);
    const [bootError, setBootError] = useState<string | null>(null);
    const [treeLoading, setTreeLoading] = useState(false);
    const [treeError, setTreeError] = useState<string | null>(null);
    const [treeSyncLoading, setTreeSyncLoading] = useState(false);
    const [treeReloadNonce, setTreeReloadNonce] = useState(0);

    const [seriesRegionId, setSeriesRegionId] = useState<number | ''>('');
    const [seriesIndicatorIds, setSeriesIndicatorIds] = useState<number[]>([]);
    const [seriesIncludeQuarterAggregates, setSeriesIncludeQuarterAggregates] = useState(true);
    const [seriesAutoCollectMissing, setSeriesAutoCollectMissing] = useState(true);
    const [seriesLoading, setSeriesLoading] = useState(false);
    const [seriesError, setSeriesError] = useState<string | null>(null);
    const [seriesResult, setSeriesResult] = useState<MonthlySeriesResultDto | null>(null);
    const [growthResult, setGrowthResult] = useState<PeriodGrowthMetricsResultDto | null>(null);
    const [seriesLastKey, setSeriesLastKey] = useState<string | null>(null);

    const [compareRegionIds, setCompareRegionIds] = useState<number[]>([]);
    const [compareIndicatorIds, setCompareIndicatorIds] = useState<number[]>([]);
    const [compareLoading, setCompareLoading] = useState(false);
    const [compareError, setCompareError] = useState<string | null>(null);
    const [compareResult, setCompareResult] = useState<RegionComparisonResultDto | null>(null);
    const [compareLastKey, setCompareLastKey] = useState<string | null>(null);

    const [subtreeRegionId, setSubtreeRegionId] = useState<number | ''>('');
    const [subtreeIndicatorIds, setSubtreeIndicatorIds] = useState<number[]>([]);
    const [subtreeLoading, setSubtreeLoading] = useState(false);
    const [subtreeError, setSubtreeError] = useState<string | null>(null);
    const [subtreeResult, setSubtreeResult] = useState<SubtreeSliceResultDto | null>(null);
    const [subtreeLastKey, setSubtreeLastKey] = useState<string | null>(null);

    const [matrixRegionIds, setMatrixRegionIds] = useState<number[]>([]);
    const [matrixIndicatorIds, setMatrixIndicatorIds] = useState<number[]>([]);
    const [matrixIncludeChildren, setMatrixIncludeChildren] = useState(false);
    const [matrixLoading, setMatrixLoading] = useState(false);
    const [matrixError, setMatrixError] = useState<string | null>(null);
    const [matrixResult, setMatrixResult] = useState<RegionIndicatorMatrixResultDto | null>(null);
    const [matrixLastKey, setMatrixLastKey] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        const load = async () => {
            try {
                setBootLoading(true);
                setBootError(null);

                const [groupResponse, regionResponse] = await Promise.all([
                    getIndicatorGroups(),
                    getRegions()
                ]);

                if (cancelled) {
                    return;
                }

                setGroups(groupResponse);
                setRegions(regionResponse);
                setGroupCode((current) => current || groupResponse[0]?.code || '');
            } catch (error) {
                if (!cancelled) {
                    setBootError(extractErrorMessage(error, 'Не удалось загрузить стартовые справочники аналитики.'));
                }
            } finally {
                if (!cancelled) {
                    setBootLoading(false);
                }
            }
        };

        void load();

        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        setTree([]);
        setTreeError(null);
        setSeriesIndicatorIds([]);
        setCompareIndicatorIds([]);
        setSubtreeIndicatorIds([]);
        setMatrixIndicatorIds([]);
        setMatrixIncludeChildren(false);

        if (!groupCode || year <= 0) {
            return;
        }

        let cancelled = false;

        const load = async () => {
            try {
                setTreeLoading(true);
                const response = await getIndicatorTree(groupCode, year);
                if (!cancelled) {
                    setTree(response);
                }
            } catch (error) {
                if (!cancelled) {
                    setTreeError(extractErrorMessage(error, 'Не удалось загрузить дерево показателей.'));
                }
            } finally {
                if (!cancelled) {
                    setTreeLoading(false);
                }
            }
        };

        void load();

        return () => {
            cancelled = true;
        };
    }, [groupCode, year, treeReloadNonce]);

    const syncTree = async () => {
        if (!groupCode || year <= 0) {
            return;
        }

        try {
            setTreeSyncLoading(true);
            await requestIndicatorTreeSync(groupCode, year);
            setTreeReloadNonce((current) => current + 1);
        } catch (error) {
            setTreeError(extractErrorMessage(error, 'Не удалось синхронизировать дерево показателей.'));
        } finally {
            setTreeSyncLoading(false);
        }
    };

    const activeIndicatorIds = scenario === 'series'
        ? seriesIndicatorIds
        : scenario === 'compare'
            ? compareIndicatorIds
            : scenario === 'subtree'
                ? subtreeIndicatorIds
                : matrixIndicatorIds;

    const setActiveIndicatorIds = (ids: number[]) => {
        if (scenario === 'series') {
            setSeriesIndicatorIds(ids);
            return;
        }
        if (scenario === 'compare') {
            setCompareIndicatorIds(ids);
            return;
        }
        if (scenario === 'subtree') {
            setSubtreeIndicatorIds(ids);
            return;
        }
        setMatrixIndicatorIds(ids);
    };

    const seriesKey = useMemo(
        () => JSON.stringify({
            groupCode,
            year,
            month,
            valueKind,
            seriesRegionId,
            seriesIndicatorIds,
            seriesIncludeQuarterAggregates,
            seriesAutoCollectMissing
        }),
        [groupCode, year, month, valueKind, seriesRegionId, seriesIndicatorIds, seriesIncludeQuarterAggregates, seriesAutoCollectMissing]
    );

    const compareKey = useMemo(
        () => JSON.stringify({groupCode, year, month, valueKind, compareRegionIds, compareIndicatorIds}),
        [groupCode, year, month, valueKind, compareRegionIds, compareIndicatorIds]
    );

    const subtreeKey = useMemo(
        () => JSON.stringify({groupCode, year, month, valueKind, subtreeRegionId, subtreeIndicatorIds}),
        [groupCode, year, month, valueKind, subtreeRegionId, subtreeIndicatorIds]
    );

    const resolvedMatrixIndicatorIds = useMemo(
        () => matrixIncludeChildren ? expandSelectedIdsWithDescendants(tree, matrixIndicatorIds) : matrixIndicatorIds,
        [matrixIncludeChildren, matrixIndicatorIds, tree]
    );

    const matrixKey = useMemo(
        () => JSON.stringify({
            groupCode,
            year,
            month,
            valueKind,
            matrixRegionIds,
            matrixIndicatorIds,
            matrixIncludeChildren,
            resolvedMatrixIndicatorIds
        }),
        [groupCode, year, month, valueKind, matrixRegionIds, matrixIndicatorIds, matrixIncludeChildren, resolvedMatrixIndicatorIds]
    );

    const loadSeries = async () => {
        if (!groupCode || seriesRegionId === '' || seriesIndicatorIds.length !== 1) {
            return;
        }

        try {
            setSeriesLoading(true);
            setSeriesError(null);

            const monthly = await buildMonthlySeries({
                groupCode,
                regionId: seriesRegionId,
                indicatorYearEntryId: seriesIndicatorIds[0],
                valueKind,
                year,
                month,
                includeQuarterAggregates: seriesIncludeQuarterAggregates,
                autoCollectMissing: seriesAutoCollectMissing
            });

            const growth = await calculatePeriodGrowthMetrics({
                groupCode,
                regionId: seriesRegionId,
                indicatorYearEntryId: seriesIndicatorIds[0],
                valueKind,
                year,
                month,
                autoCollectMissing: seriesAutoCollectMissing
            });

            setSeriesResult(monthly);
            setGrowthResult(growth);
            setSeriesLastKey(seriesKey);
        } catch (error) {
            setSeriesResult(null);
            setGrowthResult(null);
            setSeriesError(extractErrorMessage(error, 'Не удалось загрузить ряд и темпы.'));
        } finally {
            setSeriesLoading(false);
        }
    };

    const loadCompare = async () => {
        if (!groupCode || compareRegionIds.length === 0 || compareIndicatorIds.length !== 1) {
            return;
        }

        try {
            setCompareLoading(true);
            setCompareError(null);
            const response = await compareRegions({
                groupCode,
                year,
                month,
                indicatorYearEntryId: compareIndicatorIds[0],
                valueKind,
                regionIds: compareRegionIds
            });
            setCompareResult(response);
            setCompareLastKey(compareKey);
        } catch (error) {
            setCompareError(extractErrorMessage(error, 'Не удалось загрузить сравнение регионов.'));
        } finally {
            setCompareLoading(false);
        }
    };

    const loadSubtree = async () => {
        if (!groupCode || subtreeRegionId === '' || subtreeIndicatorIds.length !== 1) {
            return;
        }

        try {
            setSubtreeLoading(true);
            setSubtreeError(null);
            const response = await buildSubtreeSlice({
                groupCode,
                year,
                month,
                regionId: subtreeRegionId,
                rootIndicatorYearEntryId: subtreeIndicatorIds[0],
                valueKind
            });
            setSubtreeResult(response);
            setSubtreeLastKey(subtreeKey);
        } catch (error) {
            setSubtreeError(extractErrorMessage(error, 'Не удалось загрузить поддерево.'));
        } finally {
            setSubtreeLoading(false);
        }
    };

    const loadMatrix = async () => {
        if (!groupCode || matrixRegionIds.length === 0 || resolvedMatrixIndicatorIds.length === 0) {
            return;
        }

        try {
            setMatrixLoading(true);
            setMatrixError(null);
            const response = await buildRegionIndicatorMatrix({
                groupCode,
                year,
                month,
                regionIds: matrixRegionIds,
                indicatorYearEntryIds: resolvedMatrixIndicatorIds,
                valueKind
            });
            setMatrixResult(response);
            setMatrixLastKey(matrixKey);
        } catch (error) {
            setMatrixError(extractErrorMessage(error, 'Не удалось загрузить матрицу.'));
        } finally {
            setMatrixLoading(false);
        }
    };

    const canLoad = scenario === 'series'
        ? Boolean(groupCode && seriesRegionId !== '' && seriesIndicatorIds.length === 1)
        : scenario === 'compare'
            ? Boolean(groupCode && compareRegionIds.length > 0 && compareIndicatorIds.length === 1)
            : scenario === 'subtree'
                ? Boolean(groupCode && subtreeRegionId !== '' && subtreeIndicatorIds.length === 1)
                : Boolean(groupCode && matrixRegionIds.length > 0 && resolvedMatrixIndicatorIds.length > 0);

    const loading = scenario === 'series'
        ? seriesLoading
        : scenario === 'compare'
            ? compareLoading
            : scenario === 'subtree'
                ? subtreeLoading
                : matrixLoading;

    const matrixSelectionSummary = matrixIncludeChildren
        ? `Будет отправлено ${resolvedMatrixIndicatorIds.length} показателей с учётом потомков.`
        : null;

    return (
        <main className="page-shell">
            <header className="page-header">
                <div>
                    <h1>· ODAS ·</h1>
                    <p>Аналитический модуль</p>
                </div>
                <div className="status-badges">
                    <span className="status-badge">analysis</span>
                    <span className="status-badge">React</span>
                    <span className="status-badge">Apache ECharts</span>
                </div>
            </header>

            {bootLoading && (
                <section className="panel">
                    <div className="empty-state">Загрузка стартовых справочников аналитики…</div>
                </section>
            )}

            {bootError && !bootLoading && (
                <section className="panel">
                    <div className="error-state">{bootError}</div>
                </section>
            )}

            {!bootLoading && !bootError && (
                <>
                    <section className="panel analytics-scenario-panel">
                        <div className="panel-header align-start compact-gap">
                            <div>
                                <h2>Сценарии аналитики</h2>
                                <p>Выберите тип аналитического запроса. Для каждого сценария доступны собственные
                                    параметры и виды визуализации.</p>
                            </div>
                        </div>
                        <div className="analytics-scenario-grid">
                            {SCENARIOS.map((item) => (
                                <button
                                    key={item.key}
                                    type="button"
                                    className={`analytics-scenario-card ${scenario === item.key ? 'is-active' : ''}`}
                                    onClick={() => setScenario(item.key)}
                                >
                                    <strong>{item.title}</strong>
                                    <span>{item.description}</span>
                                </button>
                            ))}
                        </div>
                    </section>

                    <section className="panel panel-filters-layout analytics-query-panel">
                        <div className="panel-header filter-panel-header align-start">
                            <div>
                                <h2>{SCENARIOS.find((item) => item.key === scenario)?.title}</h2>
                                <p>{SCENARIOS.find((item) => item.key === scenario)?.description}</p>
                                {scenario === 'matrix' && matrixSelectionSummary && (
                                    <div className="analytics-inline-hint">{matrixSelectionSummary}</div>
                                )}
                            </div>
                            <button
                                className="primary-button"
                                type="button"
                                disabled={!canLoad || loading || treeLoading}
                                onClick={() => {
                                    if (scenario === 'series') {
                                        void loadSeries();
                                    } else if (scenario === 'compare') {
                                        void loadCompare();
                                    } else if (scenario === 'subtree') {
                                        void loadSubtree();
                                    } else {
                                        void loadMatrix();
                                    }
                                }}
                            >
                                {loading ? 'Загрузка…' : 'Построить аналитику'}
                            </button>
                        </div>

                        <div className="filter-layout-grid analytics-filter-layout-grid">
                            <div className="filter-layout-left analytics-filter-left-column">
                                <div className="analytics-shared-top-grid">
                                    <label className="field">
                                        <span>Группа показателей</span>
                                        <select value={groupCode}
                                                onChange={(event) => setGroupCode(event.target.value as IndicatorGroupCode | '')}>
                                            <option value="">Выберите группу</option>
                                            {groups.map((group) => (
                                                <option key={group.code} value={group.code}>{group.label}</option>
                                            ))}
                                        </select>
                                    </label>
                                    <label className="field field-fit-content">
                                        <span>Год</span>
                                        <input type="number" min={2000} max={2100} value={year}
                                               onChange={(event) => setYear(Number(event.target.value))}/>
                                    </label>
                                    <label className="field field-fit-content">
                                        <span>Месяц</span>
                                        <select value={month}
                                                onChange={(event) => setMonth(Number(event.target.value))}>
                                            {Object.entries(MONTH_LABELS).map(([monthValue, label]) => (
                                                <option key={monthValue} value={monthValue}>{label}</option>
                                            ))}
                                        </select>
                                    </label>
                                    <label className="field">
                                        <span>Вид значения</span>
                                        <select value={valueKind}
                                                onChange={(event) => setValueKind(event.target.value as ObservationValueKind)}>
                                            {OBSERVATION_VALUE_KIND_OPTIONS.map((option) => (
                                                <option key={option.code} value={option.code}>{option.label}</option>
                                            ))}
                                        </select>
                                    </label>
                                </div>

                                {scenario === 'series' && (
                                    <div className="analytics-form-stack">
                                        <label className="field">
                                            <span>Регион</span>
                                            <select value={seriesRegionId}
                                                    onChange={(event) => setSeriesRegionId(event.target.value ? Number(event.target.value) : '')}>
                                                <option value="">Выберите регион</option>
                                                {regions.map((region) => (
                                                    <option key={region.id}
                                                            value={region.id}>{formatRegionLabel(region)}</option>
                                                ))}
                                            </select>
                                        </label>
                                        <div className="checkbox-grid analytics-option-grid">
                                            <label className="check-row checkbox-card">
                                                <input type="checkbox" checked={seriesAutoCollectMissing}
                                                       onChange={(event) => setSeriesAutoCollectMissing(event.target.checked)}/>
                                                <span>Дособрать недостающие месяцы</span>
                                            </label>
                                            <label className="check-row checkbox-card">
                                                <input type="checkbox" checked={seriesIncludeQuarterAggregates}
                                                       onChange={(event) => setSeriesIncludeQuarterAggregates(event.target.checked)}/>
                                                <span>Рассчитывать кварталы</span>
                                            </label>
                                        </div>
                                    </div>
                                )}

                                {scenario === 'compare' && (
                                    <div className="analytics-form-stack">
                                        <RegionMultiSelect regions={regions} selectedRegionIds={compareRegionIds}
                                                           onChange={setCompareRegionIds}/>
                                    </div>
                                )}

                                {scenario === 'subtree' && (
                                    <div className="analytics-form-stack">
                                        <label className="field">
                                            <span>Регион</span>
                                            <select value={subtreeRegionId}
                                                    onChange={(event) => setSubtreeRegionId(event.target.value ? Number(event.target.value) : '')}>
                                                <option value="">Выберите регион</option>
                                                {regions.map((region) => (
                                                    <option key={region.id}
                                                            value={region.id}>{formatRegionLabel(region)}</option>
                                                ))}
                                            </select>
                                        </label>
                                    </div>
                                )}

                                {scenario === 'matrix' && (
                                    <div className="analytics-form-stack">
                                        <RegionMultiSelect regions={regions} selectedRegionIds={matrixRegionIds}
                                                           onChange={setMatrixRegionIds}/>
                                    </div>
                                )}
                            </div>

                            <div className="filter-layout-right">
                                <IndicatorTreePanel
                                    tree={tree}
                                    loading={treeLoading}
                                    error={treeError}
                                    selectedIds={activeIndicatorIds}
                                    includeChildren={scenario === 'matrix' ? matrixIncludeChildren : false}
                                    onSelectedIdsChange={setActiveIndicatorIds}
                                    onIncludeChildrenChange={setMatrixIncludeChildren}
                                    selectionMode={scenario === 'matrix' ? 'multiple' : 'single'}
                                    embedded
                                    showIncludeChildrenOption={scenario === 'matrix'}
                                    canSyncTree={Boolean(groupCode) && year > 0}
                                    syncingTree={treeSyncLoading}
                                    onSyncTree={() => void syncTree()}
                                />
                            </div>
                        </div>
                    </section>

                    {scenario === 'series' && (
                        <SeriesResultPanel
                            seriesResult={seriesResult}
                            growthResult={growthResult}
                            loading={seriesLoading}
                            error={seriesError}
                            isDirty={Boolean(seriesResult || growthResult) && seriesKey !== seriesLastKey}
                        />
                    )}

                    {scenario === 'compare' && (
                        <ComparisonResultPanel
                            result={compareResult}
                            loading={compareLoading}
                            error={compareError}
                            isDirty={Boolean(compareResult) && compareKey !== compareLastKey}
                        />
                    )}

                    {scenario === 'subtree' && (
                        <SubtreeResultPanel
                            result={subtreeResult}
                            loading={subtreeLoading}
                            error={subtreeError}
                            isDirty={Boolean(subtreeResult) && subtreeKey !== subtreeLastKey}
                        />
                    )}

                    {scenario === 'matrix' && (
                        <MatrixResultPanel
                            result={matrixResult}
                            loading={matrixLoading}
                            error={matrixError}
                            isDirty={Boolean(matrixResult) && matrixKey !== matrixLastKey}
                        />
                    )}
                </>
            )}
        </main>
    );
}

function extractErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
