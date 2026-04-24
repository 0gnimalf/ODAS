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
    treeSyncLoading: boolean;
    onGroupCodeChange: (value: IndicatorGroupCode | '') => void;
    onYearChange: (value: number) => void;
    onMonthChange: (value: number) => void;
    onRegionIdsChange: (value: number[]) => void;
    onSelectedIndicatorIdsChange: (value: number[]) => void;
    onIncludeChildrenChange: (value: boolean) => void;
    onSyncTree: () => void;
    onLoadObservations: () => void;
    canLoadObservations: boolean;
    loadingObservations: boolean;
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
                                treeSyncLoading,
                                onGroupCodeChange,
                                onYearChange,
                                onMonthChange,
                                onRegionIdsChange,
                                onSelectedIndicatorIdsChange,
                                onIncludeChildrenChange,
                                onSyncTree,
                                onLoadObservations,
                                canLoadObservations,
                                loadingObservations
                            }: FilterPanelProps) {
    return (
        <section className="panel panel-filters-layout">
            <div className="panel-header filter-panel-header">
                <div>
                    <h2>Параметры запроса</h2>
                    <p>Выберите группу, период и регионы, потом — узлы дерева показателей и затем загрузите
                        наблюдения.</p>
                </div>
                <button
                    className="primary-button"
                    type="button"
                    onClick={onLoadObservations}
                    disabled={!canLoadObservations || loadingObservations}
                >
                    {loadingObservations ? 'Загрузка…' : 'Показать данные'}
                </button>
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
                                <option value="">Выберите группу</option>
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
                        onSelectedIdsChange={onSelectedIndicatorIdsChange}
                        onIncludeChildrenChange={onIncludeChildrenChange}
                        canSyncTree={Boolean(selectedGroupCode) && selectedYear > 0}
                        syncingTree={treeSyncLoading}
                        onSyncTree={onSyncTree}
                    />
                </div>
            </div>
        </section>
    );
}
