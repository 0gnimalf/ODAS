import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, truncateLabel} from '../../shared/lib/format';
import type {PopulationByRegion} from '../../shared/lib/population';
import {countKnownPopulation} from '../../shared/lib/population';
import type {
    RegionIndicatorMatrixColumnDto,
    RegionIndicatorMatrixResultDto,
    RegionIndicatorMatrixRowDto
} from '../../shared/types/analysis';

export function MatrixResultPanel({result, loading, error, isDirty, populationByRegion, populationWarning}: {
    result: RegionIndicatorMatrixResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
    populationByRegion: PopulationByRegion;
    populationWarning: string | null;
}) {
    const [showCellLabels, setShowCellLabels] = useState(false);
    const [hideMissingColumns, setHideMissingColumns] = useState(false);

    const absoluteMatrix = useMemo(
        () => buildMatrixModel(result, populationByRegion, 'absolute', hideMissingColumns),
        [result, populationByRegion, hideMissingColumns]
    );
    const perCapitaMatrix = useMemo(
        () => buildMatrixModel(result, populationByRegion, 'perCapita', hideMissingColumns),
        [result, populationByRegion, hideMissingColumns]
    );
    const normalizedAbsoluteMatrix = useMemo(() => normalizeByColumn(absoluteMatrix), [absoluteMatrix]);
    const normalizedPerCapitaMatrix = useMemo(() => normalizeByColumn(perCapitaMatrix), [perCapitaMatrix]);

    const knownPopulation = result ? countKnownPopulation(populationByRegion, result.rows.map((row) => row.regionId)) : 0;

    return (
        <div className="results-stack">
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Матрица регионов и показателей</h2>
                        <p>Сначала показаны исходные значения, затем с учетом на населения, после этого — нормализация
                            по каждому показателю.</p>
                    </div>
                    <div className="status-badges">
                        {result && <span className="status-badge">{result.valueKindLabel}</span>}
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
                        <input type="checkbox" checked={hideMissingColumns}
                               onChange={(event) => setHideMissingColumns(event.target.checked)}/>
                        <span>Скрывать пустые столбцы</span>
                    </label>
                </div>
                {populationWarning && <div className="warning-note top-margin-16">{populationWarning}</div>}
                {!populationWarning && result && knownPopulation < result.rows.length && (
                    <div className="warning-note top-margin-16">Население найдено не для всех
                        регионов: {knownPopulation} из {result.rows.length}.</div>
                )}
            </section>

            <MatrixHeatmapSection
                title="Матрица абсолютных значений"
                description="Исходные значения из аналитического результата. Цветовая шкала общая для всей матрицы."
                matrix={absoluteMatrix}
                loading={loading}
                error={error}
                showCellLabels={showCellLabels}
                normalized={false}
                emptyMessage="Матрица абсолютных значений пуста."
            />

            <MatrixHeatmapSection
                title="Матрица на численность населения"
                description="Каждая ячейка пересчитана как значение показателя, делённое на население региона."
                matrix={perCapitaMatrix}
                loading={loading}
                error={error}
                showCellLabels={showCellLabels}
                normalized={false}
                emptyMessage="Матрица на население пуста."
            />

            <MatrixHeatmapSection
                title="Нормализованная матрица абсолютных значений"
                description="Нормализация выполнена отдельно внутри каждого показателя: минимум = 0, максимум = 100."
                matrix={normalizedAbsoluteMatrix}
                loading={loading}
                error={error}
                showCellLabels={showCellLabels}
                normalized
                emptyMessage="Нормализованная матрица абсолютных значений пуста."
            />

            <MatrixHeatmapSection
                title="Нормализованная матрица на население"
                description="Пересчитанные на население значения нормализованы отдельно по каждому показателю."
                matrix={normalizedPerCapitaMatrix}
                loading={loading}
                error={error}
                showCellLabels={showCellLabels}
                normalized
                emptyMessage="Нормализованная матрица на население пуста."
            />

            <MatrixTableSection
                title="Таблица абсолютных значений"
                description="Точная матрица исходных значений."
                matrix={absoluteMatrix}
                loading={loading}
                error={error}
            />

            <MatrixTableSection
                title="Таблица значений на население"
                description="Точная матрица значений, пересчитанных на численность населения."
                matrix={perCapitaMatrix}
                loading={loading}
                error={error}
            />

            <section className="panel result-view-panel">
                <Header title="Сводка матрицы"
                        description="Размерность, заполненность и наличие населения для нормализации."/>
                <Guard loading={loading} error={error} hasData={Boolean(result)} emptyMessage="Сводка недоступна.">
                    <div className="analytics-card-grid metrics-grid-compact">
                        <Card title="Регионов" value={result?.rows.length ?? 0}/>
                        <Card title="Показателей" value={result?.columns.length ?? 0}/>
                        <Card title="Ячеек" value={result?.cells.length ?? 0}/>
                        <Card title="Missing ячеек" value={result?.cells.filter((cell) => cell.missing).length ?? 0}/>
                        <Card title="Регионов с населением" value={`${knownPopulation} / ${result?.rows.length ?? 0}`}/>
                    </div>
                </Guard>
            </section>
        </div>
    );
}

function MatrixHeatmapSection({title, description, matrix, loading, error, showCellLabels, normalized, emptyMessage}: {
    title: string;
    description: string;
    matrix: MatrixModel | null;
    loading: boolean;
    error: string | null;
    showCellLabels: boolean;
    normalized: boolean;
    emptyMessage: string;
}) {
    return (
        <section className="panel chart-panel result-view-panel">
            <Header title={title} description={description}/>
            <Guard loading={loading} error={error}
                   hasData={Boolean(matrix && matrix.cells.some((cell) => cell.value != null))}
                   emptyMessage={emptyMessage}>
                {matrix && <ReactECharts style={{height: heatmapHeight(matrix)}}
                                         option={buildHeatmapOption(matrix, showCellLabels, normalized)} notMerge/>}
            </Guard>
        </section>
    );
}

function MatrixTableSection({title, description, matrix, loading, error}: {
    title: string;
    description: string;
    matrix: MatrixModel | null;
    loading: boolean;
    error: string | null;
}) {
    return (
        <section className="panel table-panel result-view-panel">
            <Header title={title} description={description}/>
            <Guard loading={loading} error={error}
                   hasData={Boolean(matrix && matrix.rows.length && matrix.columns.length)}
                   emptyMessage="Таблица матрицы пуста.">
                {matrix && (
                    <div className="table-wrapper table-wrapper-scroll matrix-table-scroll">
                        <table className="matrix-table">
                            <thead>
                            <tr>
                                <th className="sticky-first-column">Регион</th>
                                {matrix.columns.map((column) => (
                                    <th key={column.indicatorYearEntryId}>
                                        <span className="table-header-clamp"
                                              title={column.indicatorName}>{truncateLabel(column.indicatorName, 42)}</span>
                                    </th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {matrix.rows.map((row) => (
                                <tr key={row.regionId}>
                                    <td className="sticky-first-column">{row.regionName}</td>
                                    {matrix.columns.map((column) => {
                                        const cell = matrix.cellMap.get(makeCellKey(row.regionId, column.indicatorYearEntryId));
                                        return <td
                                            key={`${row.regionId}-${column.indicatorYearEntryId}`}>{formatObservationValue(cell?.value)}</td>;
                                    })}
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </Guard>
        </section>
    );
}

interface MatrixCellView {
    regionId: number;
    indicatorYearEntryId: number;
    value: number | null;
    missing: boolean;
}

interface MatrixModel {
    rows: RegionIndicatorMatrixRowDto[];
    columns: RegionIndicatorMatrixColumnDto[];
    cells: MatrixCellView[];
    cellMap: Map<string, MatrixCellView>;
    unitLabel: string;
}

function buildMatrixModel(
    result: RegionIndicatorMatrixResultDto | null,
    populationByRegion: PopulationByRegion,
    mode: 'absolute' | 'perCapita',
    hideMissingColumns: boolean
): MatrixModel | null {
    if (!result) return null;

    const rawCellMap = new Map(result.cells.map((cell) => [makeCellKey(cell.regionId, cell.indicatorYearEntryId), cell]));
    const cells: MatrixCellView[] = [];

    for (const row of result.rows) {
        const population = populationByRegion[row.regionId];
        for (const column of result.columns) {
            const raw = rawCellMap.get(makeCellKey(row.regionId, column.indicatorYearEntryId));
            const value = raw?.value ?? null;
            const normalizedValue = mode === 'absolute'
                ? value
                : value != null && Number.isFinite(population) && population > 0
                    ? value / population
                    : null;
            cells.push({
                regionId: row.regionId,
                indicatorYearEntryId: column.indicatorYearEntryId,
                value: normalizedValue,
                missing: (raw?.missing ?? false) || normalizedValue == null
            });
        }
    }

    const visibleColumns = hideMissingColumns
        ? result.columns.filter((column) => cells.some((cell) => cell.indicatorYearEntryId === column.indicatorYearEntryId && cell.value != null))
        : result.columns;
    const visibleColumnIds = new Set(visibleColumns.map((column) => column.indicatorYearEntryId));
    const visibleCells = cells.filter((cell) => visibleColumnIds.has(cell.indicatorYearEntryId));

    return {
        rows: result.rows,
        columns: visibleColumns,
        cells: visibleCells,
        cellMap: new Map(visibleCells.map((cell) => [makeCellKey(cell.regionId, cell.indicatorYearEntryId), cell])),
        unitLabel: mode === 'absolute' ? result.unitCodeLabel : `${result.unitCodeLabel}/чел.`
    };
}

function normalizeByColumn(matrix: MatrixModel | null): MatrixModel | null {
    if (!matrix) return null;
    const cells = matrix.cells.map((cell) => ({...cell}));
    for (const column of matrix.columns) {
        const columnCells = cells.filter((cell) => cell.indicatorYearEntryId === column.indicatorYearEntryId && cell.value != null);
        const values = columnCells.map((cell) => cell.value as number);
        if (values.length === 0) continue;
        const min = Math.min(...values);
        const max = Math.max(...values);
        for (const cell of columnCells) {
            cell.value = max === min ? 100 : (((cell.value as number) - min) / (max - min)) * 100;
        }
    }
    return {
        ...matrix,
        unitLabel: '0–100',
        cells,
        cellMap: new Map(cells.map((cell) => [makeCellKey(cell.regionId, cell.indicatorYearEntryId), cell]))
    };
}

function buildHeatmapOption(matrix: MatrixModel, showCellLabels: boolean, normalized: boolean) {
    const values = matrix.cells.map((cell) => cell.value).filter((value): value is number => value != null && Number.isFinite(value));
    const min = normalized ? 0 : Math.min(...values, 0);
    const max = normalized ? 100 : Math.max(...values, 1);
    const data = matrix.cells.map((cell) => [
        matrix.columns.findIndex((column) => column.indicatorYearEntryId === cell.indicatorYearEntryId),
        matrix.rows.findIndex((row) => row.regionId === cell.regionId),
        cell.value
    ]);

    return {
        tooltip: {
            position: 'top',
            formatter: (params: { value: [number, number, number | null] }) => {
                const [columnIndex, rowIndex, value] = params.value;
                return `${matrix.rows[rowIndex]?.regionName ?? ''}<br/>${matrix.columns[columnIndex]?.indicatorName ?? ''}<br/>${formatObservationValue(value)} ${matrix.unitLabel}`;
            }
        },
        grid: {left: 190, right: 48, top: 110, bottom: 54},
        xAxis: {
            type: 'category',
            data: matrix.columns.map((column) => truncateLabel(column.indicatorName, 26)),
            axisLabel: {interval: 0, rotate: 35, width: 120, overflow: 'truncate'}
        },
        yAxis: {
            type: 'category',
            inverse: true,
            data: matrix.rows.map((row) => truncateLabel(row.regionName, 32)),
            axisLabel: {width: 170, overflow: 'truncate'}
        },
        visualMap: {
            min,
            max,
            calculable: true,
            orient: 'horizontal',
            left: 'center',
            bottom: 0
        },
        series: [{
            type: 'heatmap',
            data,
            label: showCellLabels ? {
                show: true,
                formatter: (params: {
                    value: [number, number, number | null]
                }) => formatObservationValue(params.value[2])
            } : undefined,
            emphasis: {itemStyle: {shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.25)'}}
        }]
    };
}

function heatmapHeight(matrix: MatrixModel) {
    return Math.max(420, Math.min(980, matrix.rows.length * 30 + 180));
}

function makeCellKey(regionId: number, indicatorYearEntryId: number) {
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
    children: ReactNode;
}) {
    if (loading) return <div className="empty-state">Загрузка аналитических данных…</div>;
    if (error) return <div className="error-state">{error}</div>;
    if (!hasData) return <div className="empty-state">{emptyMessage}</div>;
    return <>{children}</>;
}

function Card({title, value}: { title: string; value: ReactNode }) {
    return <article className="analytics-kpi-card"><span className="analytics-kpi-title">{title}</span><strong
        className="analytics-kpi-value">{value}</strong></article>;
}
