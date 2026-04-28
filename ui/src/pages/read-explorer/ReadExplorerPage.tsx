import {useEffect, useMemo, useState} from 'react';
import {
    getIndicatorGroups,
    getIndicatorTree,
    getObservations,
    getRegions,
    requestIndicatorTreeSync
} from '../../shared/api/odasReadApi';
import type {
    IndicatorGroupCode,
    IndicatorGroupReadDto,
    IndicatorTreeNodeReadDto,
    ObservationReadResultDto,
    RegionReadDto
} from '../../shared/types/read';
import {FilterPanel} from '../../widgets/filter-panel/FilterPanel';
import {ResultDisplayPanel} from '../../widgets/result-views/ResultDisplayPanel';

const CURRENT_YEAR = new Date().getFullYear();

function buildRequestKey(payload: {
    groupCode: IndicatorGroupCode | '';
    year: number;
    month: number;
    regionIds: number[];
    indicatorYearEntryIds: number[];
    includeChildren: boolean;
    forceRefresh: boolean;
}): string {
    return JSON.stringify({
        ...payload,
        regionIds: [...payload.regionIds].sort((left, right) => left - right),
        indicatorYearEntryIds: [...payload.indicatorYearEntryIds].sort((left, right) => left - right)
    });
}

export function ReadExplorerPage() {
    const [groups, setGroups] = useState<IndicatorGroupReadDto[]>([]);
    const [regions, setRegions] = useState<RegionReadDto[]>([]);
    const [tree, setTree] = useState<IndicatorTreeNodeReadDto[]>([]);

    const [groupCode, setGroupCode] = useState<IndicatorGroupCode | ''>('');
    const [year, setYear] = useState<number>(CURRENT_YEAR);
    const [month, setMonth] = useState<number>(1);
    const [regionIds, setRegionIds] = useState<number[]>([]);
    const [indicatorYearEntryIds, setIndicatorYearEntryIds] = useState<number[]>([]);
    const [includeChildren, setIncludeChildren] = useState(false);
    const [forceRefresh, setForceRefresh] = useState(false);

    const [bootLoading, setBootLoading] = useState(true);
    const [bootError, setBootError] = useState<string | null>(null);

    const [treeLoading, setTreeLoading] = useState(false);
    const [treeError, setTreeError] = useState<string | null>(null);
    const [treeSyncLoading, setTreeSyncLoading] = useState(false);
    const [treeReloadNonce, setTreeReloadNonce] = useState(0);

    const [observationLoading, setObservationLoading] = useState(false);
    const [observationError, setObservationError] = useState<string | null>(null);
    const [observationResult, setObservationResult] = useState<ObservationReadResultDto | null>(null);
    const [lastAppliedRequestKey, setLastAppliedRequestKey] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        const loadInitialData = async () => {
            try {
                setBootLoading(true);
                setBootError(null);

                const [groupResponse, regionResponse] = await Promise.all([getIndicatorGroups(), getRegions()]);

                if (cancelled) {
                    return;
                }

                setGroups(groupResponse);
                setRegions(regionResponse);
            } catch (error) {
                if (!cancelled) {
                    setBootError(extractErrorMessage(error, 'Не удалось загрузить стартовые справочники.'));
                }
            } finally {
                if (!cancelled) {
                    setBootLoading(false);
                }
            }
        };

        void loadInitialData();

        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        setTree([]);
        setIndicatorYearEntryIds([]);
        setTreeError(null);

        if (!groupCode || !Number.isInteger(year) || year <= 0) {
            return;
        }

        let cancelled = false;

        const loadTree = async () => {
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

        void loadTree();

        return () => {
            cancelled = true;
        };
    }, [groupCode, year, treeReloadNonce]);

    const currentRequestKey = useMemo(
        () =>
            buildRequestKey({
                groupCode,
                year,
                month,
                regionIds,
                indicatorYearEntryIds,
                includeChildren,
                forceRefresh
            }),
        [groupCode, year, month, regionIds, indicatorYearEntryIds, includeChildren, forceRefresh]
    );

    const isDirty = Boolean(observationResult) && currentRequestKey !== lastAppliedRequestKey;
    const canLoadObservations = Boolean(groupCode) && regionIds.length > 0 && indicatorYearEntryIds.length > 0 && !treeLoading && !treeSyncLoading;

    const handleLoadObservations = async () => {
        if (!groupCode || regionIds.length === 0 || indicatorYearEntryIds.length === 0) {
            return;
        }

        try {
            setObservationLoading(true);
            setObservationError(null);

            const response = await getObservations({
                groupCode,
                year,
                month,
                regionIds,
                indicatorYearEntryIds,
                includeChildren,
                forceRefresh
            });

            setObservationResult(response);
            setLastAppliedRequestKey(currentRequestKey);
        } catch (error) {
            setObservationError(extractErrorMessage(error, 'Не удалось загрузить наблюдения.'));
        } finally {
            setObservationLoading(false);
        }
    };

    const handleSyncTree = async () => {
        if (!groupCode || year <= 0) {
            return;
        }

        try {
            setTreeSyncLoading(true);
            setTreeError(null);
            await requestIndicatorTreeSync(groupCode, year);
            setTreeReloadNonce((current) => current + 1);
        } catch (error) {
            setTreeError(extractErrorMessage(error, 'Не удалось запустить синхронизацию дерева показателей.'));
        } finally {
            setTreeSyncLoading(false);
        }
    };

    return (
        <main className="page-shell">
            <header className="page-header">
                <div>
                    <h1>Чтение данных</h1>
                    <p>Запрос сохранённых наблюдений, простое сравнение регионов и таблица сохраненных значений.</p>
                </div>
            </header>

            {bootLoading && (
                <section className="panel">
                    <div className="empty-state">Загрузка стартовых справочников…</div>
                </section>
            )}
            {bootError && !bootLoading && (
                <section className="panel">
                    <div className="error-state">{bootError}</div>
                </section>
            )}

            {!bootLoading && !bootError && (
                <>
                    <FilterPanel
                        groups={groups}
                        regions={regions}
                        tree={tree}
                        treeLoading={treeLoading}
                        treeError={treeError}
                        selectedGroupCode={groupCode}
                        selectedYear={year}
                        selectedMonth={month}
                        selectedRegionIds={regionIds}
                        selectedIndicatorIds={indicatorYearEntryIds}
                        includeChildren={includeChildren}
                        treeSyncLoading={treeSyncLoading}
                        onGroupCodeChange={(value) => {
                            setGroupCode(value);
                            setObservationResult(null);
                            setLastAppliedRequestKey(null);
                        }}
                        onYearChange={(value) => {
                            setYear(value);
                            setObservationResult(null);
                            setLastAppliedRequestKey(null);
                        }}
                        onMonthChange={setMonth}
                        onRegionIdsChange={setRegionIds}
                        onSelectedIndicatorIdsChange={setIndicatorYearEntryIds}
                        onIncludeChildrenChange={setIncludeChildren}
                        onSyncTree={() => void handleSyncTree()}
                        onLoadObservations={() => void handleLoadObservations()}
                        canLoadObservations={canLoadObservations}
                        loadingObservations={observationLoading}
                        forceRefresh={forceRefresh}
                        onForceRefreshChange={setForceRefresh}
                    />

                    <ResultDisplayPanel
                        result={observationResult}
                        loading={observationLoading}
                        error={observationError}
                        isDirty={isDirty}
                    />
                </>
            )}
        </main>
    );
}

function extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message.trim().length > 0) {
        return error.message;
    }
    return fallback;
}
