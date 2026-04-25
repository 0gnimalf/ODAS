import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue} from '../../shared/lib/format';
import type {RegionIndicatorMatrixResultDto} from '../../shared/types/analysis';

type ViewKey = 'heatmap' | 'table' | 'normalized' | 'summary';
const VIEWS: Array<{ key: ViewKey; title: string; description: string }> = [
    {key: 'heatmap', title: 'Heatmap', description: 'Основная матрица регионов и показателей.'},
    {key: 'table', title: 'Таблица-матрица', description: 'Точное табличное представление значений.'},
    {key: 'normalized', title: 'Нормализованная heatmap', description: 'Цветовая шкала по min-max выборки.'},
    {key: 'summary', title: 'Служебная сводка', description: 'Размер матрицы и заполненность.'}
];

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
    const matrix = useMemo(() => {
        if (!result) return null;
        const rowIndexById = new Map(result.rows.map((row, i) => [row.regionId, i]));
        const colIndexById = new Map(result.columns.map((col, i) => [col.indicatorYearEntryId, i]));
        const cells = result.cells.filter((cell) => !hideMissing || !cell.missing);
        const values = cells.map((cell) => cell.value).filter((v): v is number => v != null);
        return {
            rows: result.rows,
            columns: result.columns,
            cells,
            min: values.length ? Math.min(...values) : 0,
            max: values.length ? Math.max(...values) : 0,
            heatmap: cells.map((cell) => [colIndexById.get(cell.indicatorYearEntryId) ?? 0, rowIndexById.get(cell.regionId) ?? 0, cell.value, cell.missing] as [number, number, number | null, boolean])
        };
    }, [result, hideMissing]);
    const toggle = (key: ViewKey) => setEnabled((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key]);

    return <div className="results-stack">
        <section className="panel result-display-selector-panel">
            <div className="panel-header align-start compact-gap">
                <div><h2>Представления аналитики: матрица</h2><p>Heatmap, табличная матрица и сводка заполненности для
                    набора регионов и показателей.</p></div>
                <div className="status-badges"><span
                    className="status-badge">Активно: {enabled.length}</span>{isDirty &&
                    <span className="warning-badge">Параметры сценария изменены</span>}</div>
            </div>
            <div className="view-settings-grid analytics-inline-settings-grid"><label
                className="check-row checkbox-card compact-checkbox-card"><input type="checkbox"
                                                                                 checked={showCellLabels}
                                                                                 onChange={(e) => setShowCellLabels(e.target.checked)}/><span>Подписи в ячейках</span></label><label
                className="check-row checkbox-card compact-checkbox-card"><input type="checkbox" checked={hideMissing}
                                                                                 onChange={(e) => setHideMissing(e.target.checked)}/><span>Скрывать missing</span></label>
            </div>
            <div className="result-view-toggle-grid top-margin-16">{VIEWS.map((view) => {
                const active = enabledSet.has(view.key);
                return <label key={view.key} className={`result-view-toggle-card ${active ? 'is-enabled' : ''}`}>
                    <div className="result-view-toggle-main"><input type="checkbox" checked={active}
                                                                    onChange={() => toggle(view.key)}/>
                        <div><strong>{view.title}</strong><p>{view.description}</p></div>
                    </div>
                    <span
                        className={`result-view-toggle-status ${active ? 'is-enabled' : ''}`}>{active ? 'Показать' : 'Скрыто'}</span></label>;
            })}</div>
        </section>
        {enabledSet.has('heatmap') && <section className="panel chart-panel result-view-panel"><Header title="Heatmap"
                                                                                                       description="Основная матрица регионов и выбранных показателей."/><Guard
            loading={loading} error={error} hasData={Boolean(matrix?.cells.length)}
            emptyMessage="Матрица ещё не загружена.">{matrix ?
            <HeatmapChart matrix={matrix} showCellLabels={showCellLabels}/> : null}</Guard></section>}
        {enabledSet.has('normalized') &&
            <section className="panel chart-panel result-view-panel"><Header title="Нормализованная heatmap"
                                                                             description="Та же матрица, но с фокусом на относительной цветовой шкале."/><Guard
                loading={loading} error={error} hasData={Boolean(matrix?.cells.length)}
                emptyMessage="Матрица ещё не загружена.">{matrix ?
                <HeatmapChart matrix={matrix} showCellLabels={showCellLabels}/> : null}</Guard></section>}
        {enabledSet.has('table') &&
            <section className="panel table-panel result-view-panel"><Header title="Таблица-матрица"
                                                                             description="Точное табличное представление значений по строкам регионов и столбцам показателей."/><Guard
                loading={loading} error={error} hasData={Boolean(matrix && matrix.rows.length && matrix.columns.length)}
                emptyMessage="Матрица пуста.">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Регион</th>
                            {matrix?.columns.map((column) => <th
                                key={column.indicatorYearEntryId}>{column.indicatorName}</th>)}</tr>
                        </thead>
                        <tbody>{matrix?.rows.map((row) => <tr key={row.regionId}>
                            <td>{row.regionName}</td>
                            {matrix.columns.map((column) => {
                                const cell = result?.cells.find((it) => it.regionId === row.regionId && it.indicatorYearEntryId === column.indicatorYearEntryId);
                                return <td
                                    key={`${row.regionId}-${column.indicatorYearEntryId}`}>{cell?.missing ? '—' : formatObservationValue(cell?.value ?? null)}</td>;
                            })}</tr>)}</tbody>
                    </table>
                </div>
            </Guard></section>}
        {enabledSet.has('summary') && <section className="panel result-view-panel"><Header title="Служебная сводка"
                                                                                           description="Размер матрицы и заполненность результата."/><Guard
            loading={loading} error={error} hasData={Boolean(matrix)} emptyMessage="Сводка недоступна.">
            <div className="analytics-card-grid metrics-grid-compact"><Card title="Регионов"
                                                                            value={matrix?.rows.length ?? 0}/><Card
                title="Показателей" value={matrix?.columns.length ?? 0}/><Card title="Ячеек"
                                                                               value={matrix?.cells.length ?? 0}/><Card
                title="Missing ячеек" value={matrix?.cells.filter((cell) => cell.missing).length ?? 0}/></div>
        </Guard></section>}
    </div>;
}

type HeatmapMatrix = {
    rows: Array<{ regionId: number; regionName: string }>;
    columns: Array<{ indicatorYearEntryId: number; indicatorName: string }>;
    cells: Array<{ regionId: number; indicatorYearEntryId: number; value: number | null; missing: boolean }>;
    min: number;
    max: number;
    heatmap: Array<[number, number, number | null, boolean]>;
};

function HeatmapChart({matrix, showCellLabels}: { matrix: HeatmapMatrix; showCellLabels: boolean }) {
    return <ReactECharts style={{height: Math.max(320, matrix.rows.length * 42)}} option={{
        tooltip: {
            position: 'top',
            formatter: (params: { data: [number, number, number | null, boolean] }) => {
                const [x, y, value, missing] = params.data;
                return `${matrix.rows[y]?.regionName ?? ''}<br/>${matrix.columns[x]?.indicatorName ?? ''}<br/>${missing ? 'Missing' : formatObservationValue(value)}`;
            }
        },
        grid: {left: 220, right: 36, top: 24, bottom: 120},
        xAxis: {
            type: 'category',
            data: matrix.columns.map((column: { indicatorName: string }) => column.indicatorName),
            splitArea: {show: true},
            axisLabel: {interval: 0, rotate: 35}
        },
        yAxis: {
            type: 'category',
            data: matrix.rows.map((row: { regionName: string }) => row.regionName),
            splitArea: {show: true}
        },
        visualMap: {
            min: matrix.min,
            max: matrix.max,
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: 20
        },
        series: [{
            type: 'heatmap',
            data: matrix.heatmap,
            label: showCellLabels ? {
                show: true,
                formatter: ({data}: { data: [number, number, number | null] }) => formatObservationValue(data[2])
            } : undefined
        }]
    }}/>;
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
