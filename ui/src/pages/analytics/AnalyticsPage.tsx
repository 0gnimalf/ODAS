import {useEffect, useMemo, useState} from 'react';
import {getIndicatorGroups, getIndicatorTree, getObservations, getRegions} from '../../shared/api/odasReadApi';
import {requestIndicatorTreeSync} from '../../shared/api/odasReferenceApi';
import {
    buildMonthlySeries,
    buildRegionIndicatorMatrix,
    buildSubtreeSlice,
    calculatePeriodGrowthMetrics,
    compareRegions
} from '../../shared/api/odasAnalysisApi';
import {MONTH_LABELS} from '../../shared/lib/format';
import {expandSelectedIdsWithDescendants, expandSelectedIdsWithDirectChildren} from '../../shared/lib/tree';
import type {PopulationByRegion} from '../../shared/lib/population';
import {buildPopulationByRegion, findPopulationIndicatorEntryId} from '../../shared/lib/population';
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
    {key: 'series', title: 'Ряд и темпы', description: 'Помесячная динамика, квартальные агрегаты и приросты.'},
    {
        key: 'compare',
        title: 'Сравнение регионов',
        description: 'Рейтинг регионов, доли и пересчёт на численность населения.'
    },
    {key: 'subtree', title: 'Поддерево', description: 'Интерактивные пироги по уровням иерархии показателей.'},
    {
        key: 'matrix',
        title: 'Матрица',
        description: 'Абсолютные значения, значения на население и нормализация по показателям.'
    }
];

const PERCENT_UNIT_CODE = 'PERCENT';

function shouldHidePercentValueKinds(scenario: Scenario, groupCode: IndicatorGroupCode | '') {
    return scenario !== 'series' || groupCode === 'CREDIT';
}

export function AnalyticsPage() {
    const [groups, setGroups] = useState<IndicatorGroupReadDto[]>([]);
    const [regions, setRegions] = useState<RegionReadDto[]>([]);
    const [tree, setTree] = useState<IndicatorTreeNodeReadDto[]>([]);
    const [populationTree, setPopulationTree] = useState<IndicatorTreeNodeReadDto[]>([]);
    const [populationTreeError, setPopulationTreeError] = useState<string | null>(null);

    const [scenario, setScenario] = useState<Scenario>('series');
    const [groupCode, setGroupCode] = useState<IndicatorGroupCode | ''>('');
    const [year, setYear] = useState(CURRENT_YEAR);
    const [month, setMonth] = useState(1);
    const [valueKind, setValueKind] = useState<ObservationValueKind>('ACTUAL_CONSOLIDATED_SUBJECT_BUDGET');
    const [forceRefresh, setForceRefresh] = useState(false);

    const valueKindOptions = useMemo(() => {
        if (!shouldHidePercentValueKinds(scenario, groupCode)) {
            return OBSERVATION_VALUE_KIND_OPTIONS;
        }
        return OBSERVATION_VALUE_KIND_OPTIONS.filter((option) => option.unitCode !== PERCENT_UNIT_CODE);
    }, [scenario, groupCode]);

    useEffect(() => {
        if (valueKindOptions.length === 0 || valueKindOptions.some((option) => option.code === valueKind)) {
            return;
        }

        setValueKind(valueKindOptions[0].code);
    }, [valueKind, valueKindOptions]);

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
    const [comparePopulationByRegion, setComparePopulationByRegion] = useState<PopulationByRegion>({});
    const [comparePopulationWarning, setComparePopulationWarning] = useState<string | null>(null);
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
    const [matrixIncludeDirectChildrenOnly, setMatrixIncludeDirectChildrenOnly] = useState(false);
    const [matrixLoading, setMatrixLoading] = useState(false);
    const [matrixError, setMatrixError] = useState<string | null>(null);
    const [matrixResult, setMatrixResult] = useState<RegionIndicatorMatrixResultDto | null>(null);
    const [matrixPopulationByRegion, setMatrixPopulationByRegion] = useState<PopulationByRegion>({});
    const [matrixPopulationWarning, setMatrixPopulationWarning] = useState<string | null>(null);
    const [matrixLastKey, setMatrixLastKey] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        const load = async () => {
            try {
                setBootLoading(true);
                setBootError(null);
                const [groupResponse, regionResponse] = await Promise.all([getIndicatorGroups(), getRegions()]);
                if (cancelled) return;
                setGroups(groupResponse);
                setRegions(regionResponse);
            } catch (error) {
                if (!cancelled) setBootError(extractErrorMessage(error, 'Не удалось загрузить стартовые справочники аналитики.'));
            } finally {
                if (!cancelled) setBootLoading(false);
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
        setMatrixIncludeDirectChildrenOnly(false);

        if (!groupCode || year <= 0) return;
        let cancelled = false;
        const load = async () => {
            try {
                setTreeLoading(true);
                const response = await getIndicatorTree(groupCode, year);
                if (!cancelled) setTree(response);
            } catch (error) {
                if (!cancelled) setTreeError(extractErrorMessage(error, 'Не удалось загрузить дерево показателей.'));
            } finally {
                if (!cancelled) setTreeLoading(false);
            }
        };
        void load();
        return () => {
            cancelled = true;
        };
    }, [groupCode, year, treeReloadNonce]);

    useEffect(() => {
        setPopulationTree([]);
        setPopulationTreeError(null);
        if (year <= 0) return;
        let cancelled = false;
        const load = async () => {
            try {
                const response = await getIndicatorTree('OTHER', year);
                if (!cancelled) setPopulationTree(response);
            } catch (error) {
                if (!cancelled) setPopulationTreeError(extractErrorMessage(error, 'Не удалось загрузить показатель численности населения.'));
            }
        };
        void load();
        return () => {
            cancelled = true;
        };
    }, [year, treeReloadNonce]);

    const populationIndicatorEntryId = useMemo(() => findPopulationIndicatorEntryId(populationTree), [populationTree]);

    const activeIndicatorIds = scenario === 'series'
        ? seriesIndicatorIds
        : scenario === 'compare'
            ? compareIndicatorIds
            : scenario === 'subtree'
                ? subtreeIndicatorIds
                : matrixIndicatorIds;

    const setActiveIndicatorIds = (ids: number[]) => {
        if (scenario === 'series') setSeriesIndicatorIds(ids);
        else if (scenario === 'compare') setCompareIndicatorIds(ids);
        else if (scenario === 'subtree') setSubtreeIndicatorIds(ids);
        else setMatrixIndicatorIds(ids);
    };

    const resolvedMatrixIndicatorIds = useMemo(
        () => matrixIncludeChildren
            ? matrixIncludeDirectChildrenOnly
                ? expandSelectedIdsWithDirectChildren(tree, matrixIndicatorIds)
                : expandSelectedIdsWithDescendants(tree, matrixIndicatorIds)
            : matrixIndicatorIds,
        [matrixIncludeChildren, matrixIncludeDirectChildrenOnly, matrixIndicatorIds, tree]
    );

    const seriesKey = useMemo(() => JSON.stringify({
        groupCode,
        year,
        month,
        valueKind,
        seriesRegionId,
        seriesIndicatorIds,
        seriesIncludeQuarterAggregates,
        seriesAutoCollectMissing,
        forceRefresh
    }), [groupCode, year, month, valueKind, seriesRegionId, seriesIndicatorIds, seriesIncludeQuarterAggregates, seriesAutoCollectMissing, forceRefresh]);
    const compareKey = useMemo(() => JSON.stringify({
        groupCode,
        year,
        month,
        valueKind,
        compareRegionIds,
        compareIndicatorIds,
        forceRefresh
    }), [groupCode, year, month, valueKind, compareRegionIds, compareIndicatorIds, forceRefresh]);
    const subtreeKey = useMemo(() => JSON.stringify({
        groupCode,
        year,
        month,
        valueKind,
        subtreeRegionId,
        subtreeIndicatorIds,
        forceRefresh
    }), [groupCode, year, month, valueKind, subtreeRegionId, subtreeIndicatorIds, forceRefresh]);
    const matrixKey = useMemo(() => JSON.stringify({
        groupCode,
        year,
        month,
        valueKind,
        matrixRegionIds,
        matrixIndicatorIds,
        matrixIncludeChildren,
        matrixIncludeDirectChildrenOnly,
        resolvedMatrixIndicatorIds,
        forceRefresh
    }), [groupCode, year, month, valueKind, matrixRegionIds, matrixIndicatorIds, matrixIncludeChildren, matrixIncludeDirectChildrenOnly, resolvedMatrixIndicatorIds, forceRefresh]);

    const syncTree = async () => {
        if (!groupCode || year <= 0) return;
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
    const resolvePopulationIndicatorEntryId = async (): Promise<number | null> => {
        if (populationIndicatorEntryId != null) {
            return populationIndicatorEntryId;
        }

        try {
            await requestIndicatorTreeSync('OTHER', year);

            const refreshedTree = await getIndicatorTree('OTHER', year);
            setPopulationTree(refreshedTree);
            setPopulationTreeError(null);

            return findPopulationIndicatorEntryId(refreshedTree);
        } catch (error) {
            setPopulationTreeError(extractErrorMessage(error, 'Не удалось синхронизировать показатель численности населения.'));
            return null;
        }
    };

    const loadPopulation = async (regionIds: number[]): Promise<{
        values: PopulationByRegion;
        warning: string | null
    }> => {
        if (regionIds.length === 0) return {values: {}, warning: null};
        if (populationTreeError) return {values: {}, warning: populationTreeError};
        const resolvedPopulationIndicatorEntryId = await resolvePopulationIndicatorEntryId();
        if (resolvedPopulationIndicatorEntryId == null) {
            return {
                values: {},
                warning: 'Не найден показатель численности населения в группе «Другое» за выбранный год.'
            };
        }
        try {
            const response = await getObservations({
                groupCode: 'OTHER',
                year,
                month,
                regionIds,
                indicatorYearEntryIds: [resolvedPopulationIndicatorEntryId],
                valueKinds: ['POPULATION'],
                includeChildren: false,
                forceRefresh: false
            });
            return {values: buildPopulationByRegion(response), warning: null};
        } catch (error) {
            return {
                values: {},
                warning: extractErrorMessage(error, 'Не удалось загрузить численность населения для нормализации.')
            };
        }
    };

    const loadSeries = async () => {
        if (!groupCode || seriesRegionId === '' || seriesIndicatorIds.length !== 1) return;
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
                autoCollectMissing: seriesAutoCollectMissing,
                forceRefresh
            });
            const growth = await calculatePeriodGrowthMetrics({
                groupCode,
                regionId: seriesRegionId,
                indicatorYearEntryId: seriesIndicatorIds[0],
                valueKind,
                year,
                month,
                autoCollectMissing: seriesAutoCollectMissing,
                forceRefresh: false
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
        if (!groupCode || compareRegionIds.length === 0 || compareIndicatorIds.length !== 1) return;
        try {
            setCompareLoading(true);
            setCompareError(null);
            setComparePopulationWarning(null);
            const response = await compareRegions({
                groupCode,
                year,
                month,
                indicatorYearEntryId: compareIndicatorIds[0],
                valueKind,
                regionIds: compareRegionIds,
                forceRefresh
            });
            const population = await loadPopulation(compareRegionIds);
            setCompareResult(response);
            setComparePopulationByRegion(population.values);
            setComparePopulationWarning(population.warning);
            setCompareLastKey(compareKey);
        } catch (error) {
            setCompareResult(null);
            setComparePopulationByRegion({});
            setCompareError(extractErrorMessage(error, 'Не удалось загрузить сравнение регионов.'));
        } finally {
            setCompareLoading(false);
        }
    };

    const loadSubtree = async () => {
        if (!groupCode || subtreeRegionId === '' || subtreeIndicatorIds.length !== 1) return;
        try {
            setSubtreeLoading(true);
            setSubtreeError(null);
            const response = await buildSubtreeSlice({
                groupCode,
                year,
                month,
                regionId: subtreeRegionId,
                rootIndicatorYearEntryId: subtreeIndicatorIds[0],
                valueKind,
                forceRefresh
            });
            setSubtreeResult(response);
            setSubtreeLastKey(subtreeKey);
        } catch (error) {
            setSubtreeResult(null);
            setSubtreeError(extractErrorMessage(error, 'Не удалось загрузить поддерево.'));
        } finally {
            setSubtreeLoading(false);
        }
    };

    const loadMatrix = async () => {
        if (!groupCode || matrixRegionIds.length === 0 || resolvedMatrixIndicatorIds.length === 0) return;
        try {
            setMatrixLoading(true);
            setMatrixError(null);
            setMatrixPopulationWarning(null);
            const response = await buildRegionIndicatorMatrix({
                groupCode,
                year,
                month,
                regionIds: matrixRegionIds,
                indicatorYearEntryIds: resolvedMatrixIndicatorIds,
                valueKind,
                forceRefresh
            });
            const population = await loadPopulation(matrixRegionIds);
            setMatrixResult(response);
            setMatrixPopulationByRegion(population.values);
            setMatrixPopulationWarning(population.warning);
            setMatrixLastKey(matrixKey);
        } catch (error) {
            setMatrixResult(null);
            setMatrixPopulationByRegion({});
            setMatrixError(extractErrorMessage(error, 'Не удалось загрузить матрицу.'));
        } finally {
            setMatrixLoading(false);
        }
    };

    const isValueKindAvailable = valueKindOptions.some((option) => option.code === valueKind);

    const canLoad = isValueKindAvailable && (scenario === 'series'
        ? Boolean(groupCode && seriesRegionId !== '' && seriesIndicatorIds.length === 1)
        : scenario === 'compare'
            ? Boolean(groupCode && compareRegionIds.length > 0 && compareIndicatorIds.length === 1)
            : scenario === 'subtree'
                ? Boolean(groupCode && subtreeRegionId !== '' && subtreeIndicatorIds.length === 1)
                : Boolean(groupCode && matrixRegionIds.length > 0 && resolvedMatrixIndicatorIds.length > 0));

    const loading = scenario === 'series' ? seriesLoading : scenario === 'compare' ? compareLoading : scenario === 'subtree' ? subtreeLoading : matrixLoading;
    const activeScenario = SCENARIOS.find((item) => item.key === scenario) ?? SCENARIOS[0];
    const matrixSelectionSummary = scenario === 'matrix' && matrixIncludeChildren
        ? `Будет отправлено ${resolvedMatrixIndicatorIds.length} показателей: ${matrixIncludeDirectChildrenOnly ? 'выбранные узлы и их прямые потомки' : 'выбранные узлы и всё поддерево'}.`
        : null;

    const submit = () => {
        if (scenario === 'series') void loadSeries();
        else if (scenario === 'compare') void loadCompare();
        else if (scenario === 'subtree') void loadSubtree();
        else void loadMatrix();
    };

    return (
        <main className="page-shell">
            <header className="page-header page-header-compact">
                <div>
                    <h1>Аналитика</h1>
                    <p>Сценарии расчёта меняют форму запроса и набор визуализаций.</p>
                </div>
            </header>

            {bootLoading && <section className="panel">
                <div className="empty-state">Загрузка стартовых справочников аналитики…</div>
            </section>}
            {bootError && !bootLoading && <section className="panel">
                <div className="error-state">{bootError}</div>
            </section>}

            {!bootLoading && !bootError && (
                <>
                    <section className="panel analytics-workbench-panel">
                        <div className="analytics-scenario-rail">
                            {SCENARIOS.map((item) => (
                                <button key={item.key} type="button"
                                        className={`analytics-scenario-card ${scenario === item.key ? 'is-active' : ''}`}
                                        onClick={() => setScenario(item.key)}>
                                    <strong>{item.title}</strong>
                                    <span>{item.description}</span>
                                </button>
                            ))}
                        </div>

                        <div className="analytics-query-area">
                            <div className="panel-header filter-panel-header align-start">
                                <div>
                                    <h2>{activeScenario.title}</h2>
                                    <p>{activeScenario.description}</p>
                                    {matrixSelectionSummary &&
                                        <div className="analytics-inline-hint">{matrixSelectionSummary}</div>}
                                </div>
                                <div className="request-submit-controls">
                                    <label className="check-row checkbox-card force-refresh-toggle-card">
                                        <input type="checkbox" checked={forceRefresh}
                                               onChange={(event) => setForceRefresh(event.target.checked)}/>
                                        <span>Принудительно обновить из внешнего источника</span>
                                    </label>
                                    {forceRefresh &&
                                        <div className="request-warning-note">Операция может занять длительное
                                            время.</div>}
                                    <button className="primary-button" type="button"
                                            disabled={!canLoad || loading || treeLoading} onClick={submit}>
                                        {loading ? 'Загрузка…' : 'Построить аналитику'}
                                    </button>
                                </div>
                            </div>

                            <div className="analytics-query-grid">
                                <div className="analytics-query-main">
                                    <div className="analytics-shared-top-grid">
                                        <label className="field">
                                            <span>Группа показателей</span>
                                            <select value={groupCode}
                                                    onChange={(event) => setGroupCode(event.target.value as IndicatorGroupCode | '')}>
                                                <option value="" disabled>Выберите группу</option>
                                                {groups.map((group) => <option key={group.code}
                                                                               value={group.code}>{group.label}</option>)}
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
                                                {Object.entries(MONTH_LABELS).map(([monthValue, label]) => <option
                                                    key={monthValue} value={monthValue}>{label}</option>)}
                                            </select>
                                        </label>
                                        <label className="field">
                                            <span>Вид значения</span>
                                            <select value={valueKind}
                                                    onChange={(event) => setValueKind(event.target.value as ObservationValueKind)}>
                                                {valueKindOptions.map((option) => <option
                                                    key={option.code} value={option.code}>{option.label}</option>)}
                                            </select>
                                        </label>
                                    </div>

                                    {scenario === 'series' && (
                                        <div className="analytics-form-stack">
                                            <RegionMultiSelect regions={regions}
                                                               selectedRegionIds={seriesRegionId === '' ? [] : [seriesRegionId]}
                                                               onChange={(ids) => setSeriesRegionId(ids[0] ?? '')}
                                                               mode="single" label="Регион"
                                                               helperText="Один регион для построения временного ряда."/>
                                            <div className="checkbox-grid analytics-option-grid">
                                                <label className="check-row checkbox-card"><input type="checkbox"
                                                                                                  checked={seriesAutoCollectMissing}
                                                                                                  onChange={(event) => setSeriesAutoCollectMissing(event.target.checked)}/><span>Дособрать недостающие месяцы</span></label>
                                                <label className="check-row checkbox-card"><input type="checkbox"
                                                                                                  checked={seriesIncludeQuarterAggregates}
                                                                                                  onChange={(event) => setSeriesIncludeQuarterAggregates(event.target.checked)}/><span>Рассчитывать кварталы</span></label>
                                            </div>
                                        </div>
                                    )}

                                    {scenario === 'compare' &&
                                        <RegionMultiSelect regions={regions} selectedRegionIds={compareRegionIds}
                                                           onChange={setCompareRegionIds}
                                                           helperText="Выберите регионы для рейтинга и нормализации по населению."/>}

                                    {scenario === 'subtree' && <RegionMultiSelect regions={regions}
                                                                                  selectedRegionIds={subtreeRegionId === '' ? [] : [subtreeRegionId]}
                                                                                  onChange={(ids) => setSubtreeRegionId(ids[0] ?? '')}
                                                                                  mode="single" label="Регион"
                                                                                  helperText="Один регион для анализа структуры поддерева."/>}

                                    {scenario === 'matrix' &&
                                        <RegionMultiSelect regions={regions} selectedRegionIds={matrixRegionIds}
                                                           onChange={setMatrixRegionIds}
                                                           helperText="Строки матрицы: выбранные субъекты РФ."/>}
                                </div>

                                <div className="analytics-query-tree">
                                    <IndicatorTreePanel
                                        tree={tree}
                                        loading={treeLoading}
                                        error={treeError}
                                        selectedIds={activeIndicatorIds}
                                        includeChildren={scenario === 'matrix' ? matrixIncludeChildren : false}
                                        includeDirectChildrenOnly={scenario === 'matrix' ? matrixIncludeDirectChildrenOnly : false}
                                        onSelectedIdsChange={setActiveIndicatorIds}
                                        onIncludeChildrenChange={(value) => {
                                            setMatrixIncludeChildren(value);
                                            if (!value) setMatrixIncludeDirectChildrenOnly(false);
                                        }}
                                        onIncludeDirectChildrenOnlyChange={setMatrixIncludeDirectChildrenOnly}
                                        selectionMode={scenario === 'matrix' ? 'multiple' : 'single'}
                                        embedded
                                        showIncludeChildrenOption={scenario === 'matrix'}
                                        canSyncTree={Boolean(groupCode) && year > 0}
                                        syncingTree={treeSyncLoading}
                                        onSyncTree={() => void syncTree()}
                                    />
                                </div>
                            </div>
                        </div>
                    </section>

                    {scenario === 'series' && <SeriesResultPanel seriesResult={seriesResult} growthResult={growthResult}
                                                                 loading={seriesLoading} error={seriesError}
                                                                 isDirty={Boolean(seriesResult || growthResult) && seriesKey !== seriesLastKey}/>}
                    {scenario === 'compare' &&
                        <ComparisonResultPanel result={compareResult} loading={compareLoading} error={compareError}
                                               isDirty={Boolean(compareResult) && compareKey !== compareLastKey}
                                               populationByRegion={comparePopulationByRegion}
                                               populationWarning={comparePopulationWarning}/>}
                    {scenario === 'subtree' &&
                        <SubtreeResultPanel result={subtreeResult} loading={subtreeLoading} error={subtreeError}
                                            isDirty={Boolean(subtreeResult) && subtreeKey !== subtreeLastKey}/>}
                    {scenario === 'matrix' &&
                        <MatrixResultPanel result={matrixResult} loading={matrixLoading} error={matrixError}
                                           isDirty={Boolean(matrixResult) && matrixKey !== matrixLastKey}
                                           populationByRegion={matrixPopulationByRegion}
                                           populationWarning={matrixPopulationWarning}/>}
                </>
            )}
        </main>
    );
}

function extractErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
