import type {
    IndicatorGroupCode,
    IndicatorGroupReadDto,
    IndicatorTreeNodeReadDto,
    RegionReadDto
} from '../../shared/types/read';
import {IndicatorTreePanel} from '../indicator-tree/IndicatorTreePanel';
import {RegionMultiSelect} from './RegionMultiSelect';

interface FilterPanelProps {
    groups: IndicatorGroupReadDto[];
    regions: RegionReadDto[];
    tree: IndicatorTreeNodeReadDto[];
    treeLoading: boolean;
    treeError: string | null;
    selectedGroupCode: IndicatorGroupCode | '';
    selectedYear: number;
    selectedMonth: number;
    selectedRegionIds: number[];
    selectedIndicatorIds: number[];
    includeChildren: boolean;
    includeDirectChildrenOnly: boolean;
    treeSyncLoading: boolean;
    onGroupCodeChange: (value: IndicatorGroupCode | '') => void;
    onYearChange: (value: number) => void;
    onMonthChange: (value: number) => void;
    onRegionIdsChange: (value: number[]) => void;
    onSelectedIndicatorIdsChange: (value: number[]) => void;
    onIncludeChildrenChange: (value: boolean) => void;
    onIncludeDirectChildrenOnlyChange: (value: boolean) => void;
    onSyncTree: () => void;
    onLoadObservations: () => void;
    canLoadObservations: boolean;
    loadingObservations: boolean;
    forceRefresh: boolean;
    onForceRefreshChange: (value: boolean) => void;
}

const MONTH_OPTIONS = {
    1: 'Январь',
    2: 'Февраль',
    3: 'Март',
    4: 'Апрель',
    5: 'Май',
    6: 'Июнь',
    7: 'Июль',
    8: 'Август',
    9: 'Сентябрь',
    10: 'Октябрь',
    11: 'Ноябрь',
    12: 'Декабрь'
};

export function FilterPanel({
                                groups,
                                regions,
                                tree,
                                treeLoading,
                                treeError,
                                selectedGroupCode,
                                selectedYear,
                                selectedMonth,
                                selectedRegionIds,
                                selectedIndicatorIds,
                                includeChildren,
                                includeDirectChildrenOnly,
                                treeSyncLoading,
                                onGroupCodeChange,
                                onYearChange,
                                onMonthChange,
                                onRegionIdsChange,
                                onSelectedIndicatorIdsChange,
                                onIncludeChildrenChange,
                                onIncludeDirectChildrenOnlyChange,
                                onSyncTree,
                                onLoadObservations,
                                canLoadObservations,
                                loadingObservations,
                                forceRefresh,
                                onForceRefreshChange
                            }: FilterPanelProps) {
    return (
        <section className="panel panel-filters-layout">
            <div className="panel-header filter-panel-header align-start">
                <div>
                    <h2>Параметры запроса</h2>
                    <p>Выберите группу, период и регионы, потом — узлы дерева показателей и затем загрузите
                        наблюдения.</p>
                </div>
                <div className="request-submit-controls">
                    <label className="check-row checkbox-card force-refresh-toggle-card">
                        <input
                            type="checkbox"
                            checked={forceRefresh}
                            onChange={(event) => onForceRefreshChange(event.target.checked)}
                        />
                        <span>Принудительно обновить из внешнего источника</span>
                    </label>
                    {forceRefresh && (
                        <div className="request-warning-note">Операция может занять длительное время.</div>
                    )}
                    <button
                        className="primary-button"
                        type="button"
                        onClick={onLoadObservations}
                        disabled={!canLoadObservations || loadingObservations}
                    >
                        {loadingObservations ? 'Загрузка…' : 'Показать данные'}
                    </button>
                </div>
            </div>

            <div className="filter-layout-grid">
                <div className="filter-layout-left">
                    <div className="filter-top-row">
                        <label className="field">
                            <span>Группа показателей</span>
                            <select
                                value={selectedGroupCode}
                                onChange={(event) => onGroupCodeChange(event.target.value as IndicatorGroupCode | '')}
                            >
                                <option value="" disabled>Выберите группу</option>
                                {groups.map((group) => (
                                    <option key={group.code} value={group.code}>
                                        {group.label}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className="field field-year">
                            <span>Год</span>
                            <input
                                type="number"
                                min={2000}
                                max={2100}
                                value={selectedYear}
                                onChange={(event) => onYearChange(Number(event.target.value))}
                            />
                        </label>

                        <label className="field field-month">
                            <span>Месяц</span>
                            <select value={selectedMonth}
                                    onChange={(event) => onMonthChange(Number(event.target.value))}>
                                {Object.entries(MONTH_OPTIONS).map(([month, monthName]) => (
                                    <option key={month} value={month}>
                                        {monthName}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>

                    <div className="filter-region-block">
                        <RegionMultiSelect
                            regions={regions}
                            selectedRegionIds={selectedRegionIds}
                            onChange={onRegionIdsChange}
                        />
                    </div>
                </div>

                <div className="filter-layout-right">
                    <IndicatorTreePanel
                        embedded
                        tree={tree}
                        loading={treeLoading}
                        error={treeError}
                        selectedIds={selectedIndicatorIds}
                        includeChildren={includeChildren}
                        includeDirectChildrenOnly={includeDirectChildrenOnly}
                        onSelectedIdsChange={onSelectedIndicatorIdsChange}
                        onIncludeChildrenChange={onIncludeChildrenChange}
                        onIncludeDirectChildrenOnlyChange={onIncludeDirectChildrenOnlyChange}
                        canSyncTree={Boolean(selectedGroupCode) && selectedYear > 0}
                        syncingTree={treeSyncLoading}
                        onSyncTree={onSyncTree}
                    />
                </div>
            </div>
        </section>
    );
}
