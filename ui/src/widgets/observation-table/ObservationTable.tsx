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
                {truncateLabel(observation.indicatorName, 96)}
            </span>
        )
    },
    {key: 'valueKindLabel', label: 'Вид значения', render: (observation) => observation.valueKindLabel},
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
            if (valueKindFilter && observation.valueKindLabel !== valueKindFilter) {
                return false;
            }
            if (unitCodeFilter && observation.unitCodeLabel !== unitCodeFilter) {
                return false;
            }
            if (!normalizedSearchText) {
                return true;
            }

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

    const toggleColumn = (columnKey: TableColumnKey) => {
        setVisibleColumns((current) => {
            if (current.includes(columnKey)) {
                return current.filter((item) => item !== columnKey);
            }
            return [...current, columnKey];
        });
    };

    return (
        <section className="panel table-panel result-view-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>Таблица наблюдений</h2>
                    <p>Гибкое табличное представление с настройкой столбцов, сортировки и фильтров.</p>
                </div>
                <div className="result-view-actions">
                    {isDirty && <div className="warning-badge">Фильтры запроса изменены</div>}
                    <button type="button" className="secondary-button"
                            onClick={() => setSettingsVisible((current) => !current)}>
                        {settingsVisible ? 'Скрыть настройки' : 'Настройки таблицы'}
                    </button>
                </div>
            </div>

            {settingsVisible && (
                <div className="view-settings-card">
                    <div className="view-settings-grid">
                        <label className="field">
                            <span>Поиск по строкам</span>
                            <input
                                type="search"
                                value={searchText}
                                onChange={(event) => setSearchText(event.target.value)}
                                placeholder="Регион, показатель, значение…"
                            />
                        </label>

                        <label className="field">
                            <span>Фильтр по виду значения</span>
                            <select value={valueKindFilter}
                                    onChange={(event) => setValueKindFilter(event.target.value)}>
                                <option value="">Все виды</option>
                                {valueKindOptions.map((option) => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className="field">
                            <span>Фильтр по единице</span>
                            <select value={unitCodeFilter} onChange={(event) => setUnitCodeFilter(event.target.value)}>
                                <option value="">Все единицы</option>
                                {unitCodeOptions.map((option) => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className="field">
                            <span>Сортировка</span>
                            <select value={sortBy}
                                    onChange={(event) => setSortBy(event.target.value as TableColumnKey)}>
                                {COLUMN_DEFINITIONS.map((column) => (
                                    <option key={column.key} value={column.key}>
                                        {column.label}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label className="field field-fit-content">
                            <span>Направление</span>
                            <select value={sortDirection}
                                    onChange={(event) => setSortDirection(event.target.value as TableSortDirection)}>
                                <option value="asc">По возрастанию</option>
                                <option value="desc">По убыванию</option>
                            </select>
                        </label>
                    </div>

                    <div className="column-settings-block">
                        <div className="selector-header-row compact-bottom-gap">
                            <strong>Отображаемые поля</strong>
                            <div className="inline-actions wrap">
                                <button type="button" onClick={() => setVisibleColumns(DEFAULT_VISIBLE_COLUMNS)}>
                                    Все поля
                                </button>
                                <button type="button" onClick={() => setVisibleColumns([])}>
                                    Очистить
                                </button>
                            </div>
                        </div>
                        <div className="checkbox-grid">
                            {COLUMN_DEFINITIONS.map((column) => (
                                <label key={column.key} className="check-row checkbox-card">
                                    <input
                                        type="checkbox"
                                        checked={visibleColumnSet.has(column.key)}
                                        onChange={() => toggleColumn(column.key)}
                                    />
                                    <span>{column.label}</span>
                                </label>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="empty-state">Загрузка наблюдений…</div>
            ) : error ? (
                <div className="error-state">{error}</div>
            ) : !result ? (
                <div className="empty-state">Запрос ещё не выполнялся.</div>
            ) : filteredObservations.length === 0 ? (
                <div className="empty-state">По текущим настройкам таблицы ничего не найдено.</div>
            ) : visibleColumnDefinitions.length === 0 ? (
                <div className="empty-state">Выберите хотя бы одно поле для отображения таблицы.</div>
            ) : (
                <>
                    <div className="result-view-summary-row">
                        <span className="status-badge">После фильтрации: {filteredObservations.length}</span>
                        <span className="status-badge">Всего в ответе: {result.total}</span>
                    </div>
                    <div className="table-wrapper">
                        <table>
                            <thead>
                            <tr>
                                {visibleColumnDefinitions.map((column) => (
                                    <th key={column.key}>{column.label}</th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {filteredObservations.map((observation) => (
                                <tr key={observation.observationId}>
                                    {visibleColumnDefinitions.map((column) => (
                                        <td key={column.key}>{column.render(observation)}</td>
                                    ))}
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </section>
    );
}

function compareObservations(
    left: ObservationReadDto,
    right: ObservationReadDto,
    sortBy: TableColumnKey,
    sortDirection: TableSortDirection
): number {
    const directionMultiplier = sortDirection === 'asc' ? 1 : -1;

    if (sortBy === 'value') {
        return (left.value - right.value) * directionMultiplier;
    }

    if (sortBy === 'datasetCollectionId') {
        return (left.datasetCollectionId - right.datasetCollectionId) * directionMultiplier;
    }

    const leftValue = getComparableStringValue(left, sortBy);
    const rightValue = getComparableStringValue(right, sortBy);
    return leftValue.localeCompare(rightValue, 'ru') * directionMultiplier;
}

function getComparableStringValue(observation: ObservationReadDto, sortBy: Exclude<TableColumnKey, 'value' | 'datasetCollectionId'>): string {
    switch (sortBy) {
        case 'regionName':
            return observation.regionName;
        case 'indicatorName':
            return observation.indicatorName;
        case 'valueKindLabel':
            return observation.valueKindLabel;
        case 'unitCodeLabel':
            return observation.unitCode;
    }
}
