import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import {formatObservationValue, truncateLabel} from '../../shared/lib/format';
import type {ObservationReadDto, ObservationReadResultDto} from '../../shared/types/read';

type TableColumnKey =
    | 'regionName'
    | 'indicatorName'
    | 'valueKindLabel'
    | 'unitCodeLabel'
    | 'value'
    | 'datasetCollectionId';

type TableSortDirection = 'asc' | 'desc';

interface ObservationTableProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}

interface ColumnDefinition {
    key: TableColumnKey;
    label: string;
    render: (observation: ObservationReadDto) => ReactNode;
}

const COLUMN_DEFINITIONS: ColumnDefinition[] = [
    {key: 'regionName', label: 'Регион', render: (observation) => observation.regionName},
    {
        key: 'indicatorName',
        label: 'Показатель',
        render: (observation) => (
            <span className="table-cell-clamp observation-indicator-cell" title={observation.indicatorName}>
                {truncateLabel(observation.indicatorName, 92)}
            </span>
        )
    },
    {
        key: 'valueKindLabel',
        label: 'Вид значения',
        render: (observation) => truncateLabel(observation.valueKindLabel, 52)
    },
    {key: 'unitCodeLabel', label: 'Ед. изм.', render: (observation) => observation.unitCodeLabel},
    {key: 'value', label: 'Значение', render: (observation) => formatObservationValue(observation.value)},
    {key: 'datasetCollectionId', label: 'Dataset', render: (observation) => observation.datasetCollectionId}
];

const DEFAULT_VISIBLE_COLUMNS: TableColumnKey[] = COLUMN_DEFINITIONS.map((column) => column.key);

export function ObservationTable({result, loading, error, isDirty}: ObservationTableProps) {
    const [settingsVisible, setSettingsVisible] = useState(false);
    const [visibleColumns, setVisibleColumns] = useState<TableColumnKey[]>(DEFAULT_VISIBLE_COLUMNS);
    const [sortBy, setSortBy] = useState<TableColumnKey>('regionName');
    const [sortDirection, setSortDirection] = useState<TableSortDirection>('asc');
    const [searchText, setSearchText] = useState('');
    const [valueKindFilter, setValueKindFilter] = useState('');
    const [unitCodeFilter, setUnitCodeFilter] = useState('');
    const [visibleRowLimit, setVisibleRowLimit] = useState(20);

    const visibleColumnSet = useMemo(() => new Set(visibleColumns), [visibleColumns]);

    const valueKindOptions = useMemo(
        () => Array.from(new Set(result?.observations.map((item) => item.valueKindLabel) ?? [])).sort((left, right) => left.localeCompare(right, 'ru')),
        [result]
    );
    const unitCodeOptions = useMemo(
        () => Array.from(new Set(result?.observations.map((item) => item.unitCodeLabel) ?? [])).sort((left, right) => left.localeCompare(right, 'ru')),
        [result]
    );

    const filteredObservations = useMemo(() => {
        const observations = result?.observations ?? [];
        const normalizedSearchText = searchText.trim().toLowerCase();

        const next = observations.filter((observation) => {
            if (valueKindFilter && observation.valueKindLabel !== valueKindFilter) return false;
            if (unitCodeFilter && observation.unitCodeLabel !== unitCodeFilter) return false;
            if (!normalizedSearchText) return true;

            const searchTokens = [
                observation.regionName,
                observation.indicatorName,
                observation.valueKindLabel,
                observation.unitCodeLabel,
                String(observation.datasetCollectionId),
                formatObservationValue(observation.value)
            ];
            return searchTokens.some((token) => token.toLowerCase().includes(normalizedSearchText));
        });

        next.sort((left, right) => compareObservations(left, right, sortBy, sortDirection));
        return next;
    }, [result, searchText, valueKindFilter, unitCodeFilter, sortBy, sortDirection]);

    const visibleColumnDefinitions = COLUMN_DEFINITIONS.filter((column) => visibleColumnSet.has(column.key));
    const tableMaxHeight = Math.max(8, visibleRowLimit) * 44 + 48;

    const toggleColumn = (columnKey: TableColumnKey) => {
        setVisibleColumns((current) => current.includes(columnKey)
            ? current.filter((item) => item !== columnKey)
            : [...current, columnKey]);
    };

    return (
        <section className="panel table-panel result-view-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>Таблица наблюдений</h2>
                    <p>Гибкое табличное представление с настройкой столбцов, сортировки и фильтров,</p>
                    <p>а область данных прокручивается внутри блока.</p>
                </div>
                <div className="result-view-actions">
                    {isDirty && <span className="warning-badge">Фильтры запроса изменены</span>}
                    <button type="button" className="secondary-button"
                            onClick={() => setSettingsVisible((current) => !current)}>
                        {settingsVisible ? 'Скрыть настройки' : 'Настройки таблицы'}
                    </button>
                </div>
            </div>

            {settingsVisible && (
                <div className="view-settings-card">
                    <div className="view-settings-grid table-settings-grid">
                        <label className="field">
                            <span>Поиск</span>
                            <input type="search" value={searchText}
                                   onChange={(event) => setSearchText(event.target.value)}
                                   placeholder="Регион, показатель, значение…"/>
                        </label>
                        <label className="field">
                            <span>Вид значения</span>
                            <select value={valueKindFilter}
                                    onChange={(event) => setValueKindFilter(event.target.value)}>
                                <option value="">Все</option>
                                {valueKindOptions.map((option) => <option key={option}
                                                                          value={option}>{option}</option>)}
                            </select>
                        </label>
                        <label className="field">
                            <span>Единица измерения</span>
                            <select value={unitCodeFilter} onChange={(event) => setUnitCodeFilter(event.target.value)}>
                                <option value="">Все</option>
                                {unitCodeOptions.map((option) => <option key={option} value={option}>{option}</option>)}
                            </select>
                        </label>
                        <label className="field field-fit-content">
                            <span>Строк видно</span>
                            <input type="number" min={5} max={150} value={visibleRowLimit}
                                   onChange={(event) => setVisibleRowLimit(clamp(Number(event.target.value) || 25, 8, 80))}/>
                        </label>
                        <label className="field">
                            <span>Сортировать по</span>
                            <select value={sortBy}
                                    onChange={(event) => setSortBy(event.target.value as TableColumnKey)}>
                                {COLUMN_DEFINITIONS.map((column) => <option key={column.key}
                                                                            value={column.key}>{column.label}</option>)}
                            </select>
                        </label>
                        <label className="field field-fit-content">
                            <span>Порядок</span>
                            <select value={sortDirection}
                                    onChange={(event) => setSortDirection(event.target.value as TableSortDirection)}>
                                <option value="asc">По возрастанию</option>
                                <option value="desc">По убыванию</option>
                            </select>
                        </label>
                    </div>
                    <div className="column-toggle-row">
                        {COLUMN_DEFINITIONS.map((column) => (
                            <label key={column.key}
                                   className={`chip-toggle ${visibleColumnSet.has(column.key) ? 'is-active' : ''}`}>
                                <input type="checkbox" checked={visibleColumnSet.has(column.key)}
                                       onChange={() => toggleColumn(column.key)}/>
                                <span>{column.label}</span>
                            </label>
                        ))}
                    </div>
                </div>
            )}

            <Guard loading={loading} error={error} hasData={filteredObservations.length > 0}
                   emptyMessage="Наблюдения ещё не загружены или не соответствуют фильтрам.">
                <div className="table-summary-row">
                    <span>Всего: {result?.total ?? 0}</span>
                    <span>После фильтров: {filteredObservations.length}</span>
                    <span>Видимая область: до {visibleRowLimit} строк</span>
                </div>
                <div className="table-wrapper table-wrapper-scroll" style={{maxHeight: tableMaxHeight}}>
                    <table>
                        <thead>
                        <tr>
                            {visibleColumnDefinitions.map((column) => <th key={column.key}>{column.label}</th>)}
                        </tr>
                        </thead>
                        <tbody>
                        {filteredObservations.map((observation) => (
                            <tr key={observation.observationId}>
                                {visibleColumnDefinitions.map((column) => <td
                                    key={column.key}>{column.render(observation)}</td>)}
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </Guard>
        </section>
    );
}

function compareObservations(left: ObservationReadDto, right: ObservationReadDto, sortBy: TableColumnKey, direction: TableSortDirection) {
    const multiplier = direction === 'asc' ? 1 : -1;
    if (sortBy === 'value' || sortBy === 'datasetCollectionId') {
        return ((left[sortBy] as number) - (right[sortBy] as number)) * multiplier;
    }
    return String(left[sortBy]).localeCompare(String(right[sortBy]), 'ru') * multiplier;
}

function Guard({loading, error, hasData, emptyMessage, children}: {
    loading: boolean;
    error: string | null;
    hasData: boolean;
    emptyMessage: string;
    children: ReactNode;
}) {
    if (loading) return <div className="empty-state">Загрузка таблицы…</div>;
    if (error) return <div className="error-state">{error}</div>;
    if (!hasData) return <div className="empty-state">{emptyMessage}</div>;
    return <>{children}</>;
}

function clamp(value: number, min: number, max: number) {
    return Math.min(max, Math.max(min, value));
}
