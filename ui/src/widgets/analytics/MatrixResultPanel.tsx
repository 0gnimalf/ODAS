import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, truncateLabel} from '../../shared/lib/format';
import type {MatrixCellDto, RegionIndicatorMatrixResultDto} from '../../shared/types/analysis';

type ViewKey = 'heatmap' | 'table' | 'normalized' | 'summary';

const VIEWS: Array<{ key: ViewKey; title: string; description: string }> = [
    {key: 'heatmap', title: 'Heatmap', description: 'Основная матрица регионов и показателей.'},
    {key: 'table', title: 'Таблица-матрица', description: 'Точное табличное представление значений.'},
    {key: 'normalized', title: 'Нормализованная heatmap', description: 'Цветовая шкала в пределах каждого показателя.'},
    {key: 'summary', title: 'Служебная сводка', description: 'Размер матрицы и заполненность.'}
];

const HEATMAP_COLORS = ['#f7fbff', '#deebf7', '#9ecae1', '#3182bd', '#08519c'];

export function MatrixResultPanel({result, loading, error, isDirty}: {
    result: RegionIndicatorMatrixResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [enabled, setEnabled] = useState<ViewKey[]>(['heatmap', 'table', 'summary']);
    const [showCellLabels, setShowCellLabels] = useState(false);
    const [hideMissing, setHideMissing] = useState(false);

    const enabledSet = useMemo(() => new Set(enabled), [enabled]);
    const matrix = useMemo(() => buildMatrixModel(result, hideMissing), [result, hideMissing]);

    const toggle = (key: ViewKey) => {
        setEnabled((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key]);
    };

    return (
        <div className="results-stack">
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Представления аналитики: матрица</h2>
                        <p>Heatmap, табличная матрица и сводка заполненности для набора регионов и показателей.</p>
                    </div>
                    <div className="status-badges">
                        <span className="status-badge">Активно: {enabled.length}</span>
                        {isDirty && <span className="warning-badge">Параметры сценария изменены</span>}
                    </div>
                </div>

                <div className="view-settings-grid analytics-inline-settings-grid">
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input type="checkbox" checked={showCellLabels}
                               onChange={(event) => setShowCellLabels(event.target.checked)}/>
                        <span>Подписи в ячейках</span>
                    </label>
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input type="checkbox" checked={hideMissing}
                               onChange={(event) => setHideMissing(event.target.checked)}/>
                        <span>Скрывать missing</span>
                    </label>
                </div>

                <div className="result-view-toggle-grid top-margin-16">
                    {VIEWS.map((view) => {
                        const active = enabledSet.has(view.key);
                        return (
                            <label key={view.key} className={`result-view-toggle-card ${active ? 'is-enabled' : ''}`}>
                                <div className="result-view-toggle-main">
                                    <input type="checkbox" checked={active} onChange={() => toggle(view.key)}/>
                                    <div>
                                        <strong>{view.title}</strong>
                                        <p>{view.description}</p>
                                    </div>
                                </div>
                                <span
                                    className={`result-view-toggle-status ${active ? 'is-enabled' : ''}`}>{active ? 'Показать' : 'Скрыто'}</span>
                            </label>
                        );
                    })}
                </div>
            </section>

            {enabledSet.has('heatmap') && (
                <section className="panel chart-panel result-view-panel">
                    <Header title="Heatmap"
                            description="Основная матрица регионов и выбранных показателей. Цвет нормализуется по всей выборке."/>
                    <Guard loading={loading} error={error} hasData={Boolean(matrix?.cells.length)}
                           emptyMessage="Матрица ещё не загружена.">
                        {matrix ? <HeatmapChart matrix={matrix} showCellLabels={showCellLabels}
                                                normalizedByColumn={false}/> : null}
                    </Guard>
                </section>
            )}

            {enabledSet.has('normalized') && (
                <section className="panel chart-panel result-view-panel">
                    <Header title="Нормализованная heatmap"
                            description="Цвет нормализуется отдельно внутри каждого показателя, чтобы отличия были заметнее."/>
                    <Guard loading={loading} error={error} hasData={Boolean(matrix?.cells.length)}
                           emptyMessage="Матрица ещё не загружена.">
                        {matrix ?
                            <HeatmapChart matrix={matrix} showCellLabels={showCellLabels} normalizedByColumn/> : null}
                    </Guard>
                </section>
            )}

            {enabledSet.has('table') && (
                <section className="panel table-panel result-view-panel">
                    <Header title="Таблица-матрица"
                            description="Точное табличное представление значений по строкам регионов и столбцам показателей."/>
                    <Guard loading={loading} error={error}
                           hasData={Boolean(matrix && matrix.rows.length && matrix.columns.length)}
                           emptyMessage="Матрица пуста.">
                        <div className="table-wrapper">
                            <table>
                                <thead>
                                <tr>
                                    <th>Регион</th>
                                    {matrix?.columns.map((column) => (
                                        <th key={column.indicatorYearEntryId}>
                      <span className="table-header-clamp" title={column.indicatorName}>
                        {truncateLabel(column.indicatorName, 42)}
                      </span>
                                        </th>
                                    ))}
                                </tr>
                                </thead>
                                <tbody>
                                {matrix?.rows.map((row) => (
                                    <tr key={row.regionId}>
                                        <td>{row.regionName}</td>
                                        {matrix.columns.map((column) => {
                                            const cell = matrix.cellMap.get(makeCellKey(row.regionId, column.indicatorYearEntryId));
                                            return (
                                                <td key={`${row.regionId}-${column.indicatorYearEntryId}`}>
                                                    {cell?.missing ? '—' : formatObservationValue(cell?.value ?? null)}
                                                </td>
                                            );
                                        })}
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </Guard>
                </section>
            )}

            {enabledSet.has('summary') && (
                <section className="panel result-view-panel">
                    <Header title="Служебная сводка" description="Размер матрицы и заполненность результата."/>
                    <Guard loading={loading} error={error} hasData={Boolean(matrix)} emptyMessage="Сводка недоступна.">
                        <div className="analytics-card-grid metrics-grid-compact">
                            <Card title="Регионов" value={matrix?.rows.length ?? 0}/>
                            <Card title="Показателей" value={matrix?.columns.length ?? 0}/>
                            <Card title="Ячеек" value={matrix?.cells.length ?? 0}/>
                            <Card title="Missing ячеек"
                                  value={matrix?.cells.filter((cell) => cell.missing).length ?? 0}/>
                        </div>
                    </Guard>
                </section>
            )}
        </div>
    );
}

type HeatmapPoint = {
    value: [number, number, number | null, number | null, number];
    itemStyle?: { color: string };
};

type HeatmapMatrix = {
    rows: Array<{ regionId: number; regionName: string }>;
    columns: Array<{ indicatorYearEntryId: number; indicatorName: string }>;
    cells: MatrixCellDto[];
    cellMap: Map<string, MatrixCellDto>;
    globalHeatmap: HeatmapPoint[];
    columnNormalizedHeatmap: HeatmapPoint[];
};

function buildMatrixModel(result: RegionIndicatorMatrixResultDto | null, hideMissing: boolean): HeatmapMatrix | null {
    if (!result) {
        return null;
    }

    const rowIndexById = new Map(result.rows.map((row, index) => [row.regionId, index]));
    const colIndexById = new Map(result.columns.map((column, index) => [column.indicatorYearEntryId, index]));
    const cells = result.cells.filter((cell) => !hideMissing || !cell.missing);
    const cellMap = new Map(cells.map((cell) => [makeCellKey(cell.regionId, cell.indicatorYearEntryId), cell]));

    const numericValues = cells
        .map((cell) => cell.value)
        .filter((value): value is number => value != null && Number.isFinite(value));

    const globalStats = getStats(numericValues);

    const valuesByColumn = new Map<number, number[]>();
    cells.forEach((cell) => {
        if (cell.value == null || !Number.isFinite(cell.value)) {
            return;
        }
        const bucket = valuesByColumn.get(cell.indicatorYearEntryId) ?? [];
        bucket.push(cell.value);
        valuesByColumn.set(cell.indicatorYearEntryId, bucket);
    });

    const columnStatsById = new Map<number, { min: number; max: number }>();
    valuesByColumn.forEach((values, indicatorYearEntryId) => {
        columnStatsById.set(indicatorYearEntryId, getStats(values));
    });

    const toHeatmapPoint = (cell: MatrixCellDto, columnScoped: boolean): HeatmapPoint => {
        const columnIndex = colIndexById.get(cell.indicatorYearEntryId) ?? 0;
        const rowIndex = rowIndexById.get(cell.regionId) ?? 0;

        if (cell.missing || cell.value == null || !Number.isFinite(cell.value)) {
            return {
                value: [columnIndex, rowIndex, null, null, 1],
                itemStyle: {color: '#eef2f8'}
            };
        }

        const stats = columnScoped ? columnStatsById.get(cell.indicatorYearEntryId) ?? globalStats : globalStats;
        const normalizedValue = normalizeToPercent(cell.value, stats.min, stats.max);

        return {
            value: [columnIndex, rowIndex, normalizedValue, cell.value, 0]
        };
    };

    return {
        rows: result.rows,
        columns: result.columns,
        cells,
        cellMap,
        globalHeatmap: cells.map((cell) => toHeatmapPoint(cell, false)),
        columnNormalizedHeatmap: cells.map((cell) => toHeatmapPoint(cell, true))
    };
}

function getStats(values: number[]): { min: number; max: number } {
    if (values.length === 0) {
        return {min: 0, max: 100};
    }

    const min = Math.min(...values);
    const max = Math.max(...values);
    return {min, max};
}

function normalizeToPercent(value: number, min: number, max: number): number {
    if (max === min) {
        return 50;
    }
    return Math.max(0, Math.min(100, ((value - min) / (max - min)) * 100));
}

function HeatmapChart({matrix, showCellLabels, normalizedByColumn}: {
    matrix: HeatmapMatrix;
    showCellLabels: boolean;
    normalizedByColumn: boolean
}) {
    const data = normalizedByColumn ? matrix.columnNormalizedHeatmap : matrix.globalHeatmap;

    return (
        <ReactECharts
            style={{height: Math.max(320, matrix.rows.length * 42)}}
            option={{
                tooltip: {
                    position: 'top',
                    formatter: (params: {
                        data: { value: [number, number, number | null, number | null, number] }
                    }) => {
                        const [x, y, , rawValue, missingFlag] = params.data.value;
                        const column = matrix.columns[x];
                        const row = matrix.rows[y];
                        return `${row?.regionName ?? ''}<br/>${column?.indicatorName ?? ''}<br/>${missingFlag === 1 ? 'Missing' : formatObservationValue(rawValue)}`;
                    }
                },
                grid: {left: 220, right: 36, top: 24, bottom: 140},
                xAxis: {
                    type: 'category',
                    data: matrix.columns.map((column) => column.indicatorName),
                    splitArea: {show: true},
                    axisLabel: {
                        interval: 0,
                        rotate: 35,
                        formatter: (value: string) => truncateLabel(value, 24)
                    }
                },
                yAxis: {
                    type: 'category',
                    data: matrix.rows.map((row) => row.regionName),
                    splitArea: {show: true}
                },
                visualMap: {
                    min: 0,
                    max: 100,
                    calculable: true,
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 20,
                    inRange: {color: HEATMAP_COLORS},
                    formatter: (value: number) => `${Math.round(value)}%`
                },
                series: [{
                    type: 'heatmap',
                    data,
                    label: showCellLabels ? {
                        show: true,
                        formatter: ({data: item}: {
                            data: { value: [number, number, number | null, number | null, number] }
                        }) => formatObservationValue(item.value[3])
                    } : undefined,
                    emphasis: {itemStyle: {shadowBlur: 10, shadowColor: 'rgba(23, 32, 51, 0.18)'}}
                }]
            }}
        />
    );
}

function makeCellKey(regionId: number, indicatorYearEntryId: number): string {
    return `${regionId}:${indicatorYearEntryId}`;
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
    children: ReactNode
}) {
    if (loading) return <div className="empty-state">Загрузка аналитических данных…</div>;
    if (error) return <div className="error-state">{error}</div>;
    if (!hasData) return <div className="empty-state">{emptyMessage}</div>;
    return <>{children}</>;
}

function Card({title, value}: { title: string; value: number }) {
    return <article className="analytics-kpi-card"><span className="analytics-kpi-title">{title}</span><strong
        className="analytics-kpi-value">{value}</strong></article>;
}
