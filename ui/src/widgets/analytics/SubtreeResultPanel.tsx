import type {ReactNode} from 'react';
import {useEffect, useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {
    buildContainedTooltip,
    buildTooltipHtml,
    CHART_COLOR_PALETTE,
    formatObservationValue,
    formatPercentValue,
    truncateLabel,
    wrapChartLabel
} from '../../shared/lib/format';
import type {SubtreeSliceNodeDto, SubtreeSliceResultDto} from '../../shared/types/analysis';
import {type AnalyticsViewDefinition, AnalyticsViewSelector} from './AnalyticsViewSelector';

type SubtreeViewKey = 'pies' | 'table';

const SUBTREE_VIEWS: Array<AnalyticsViewDefinition<SubtreeViewKey>> = [
    {key: 'pies', title: 'Иерархия', description: 'Пошаговое раскрытие детей выбранного корня.'},
    {key: 'table', title: 'Таблица поддерева', description: 'Значение, доли к родителю и главному корню.'}
];

const SUBTREE_TABLE_ROW_HEIGHT = 48;
const SUBTREE_TABLE_HEIGHT = 520;
const SUBTREE_TABLE_OVERSCAN = 10;

export function SubtreeResultPanel({result, loading, error, isDirty}: {
    result: SubtreeSliceResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [enabledViews, setEnabledViews] = useState<SubtreeViewKey[]>(['pies', 'table']);
    const [drillPath, setDrillPath] = useState<number[]>([]);
    const [showMissing, setShowMissing] = useState(false);
    const [tableScrollTop, setTableScrollTop] = useState(0);

    useEffect(() => {
        setDrillPath([]);
        setTableScrollTop(0);
    }, [result?.rootIndicatorYearEntryId]);

    const enabledViewSet = useMemo(() => new Set(enabledViews), [enabledViews]);
    const toggleView = (view: SubtreeViewKey) => setEnabledViews((current) => current.includes(view) ? current.filter((item) => item !== view) : [...current, view]);

    const model = useMemo(() => buildSubtreeModel(result, showMissing), [result, showMissing]);
    const tableVirtualWindow = useMemo(
        () => buildVirtualWindow(model.nodes, tableScrollTop, SUBTREE_TABLE_ROW_HEIGHT, SUBTREE_TABLE_HEIGHT, SUBTREE_TABLE_OVERSCAN),
        [model.nodes, tableScrollTop]
    );
    const activeRootIds = useMemo(() => {
        if (!result) return [];
        const validPath: number[] = [];
        for (const nodeId of drillPath) {
            const node = model.nodeById.get(nodeId);
            if (!node || model.childrenByParent.get(nodeId)?.length === 0) break;
            validPath.push(nodeId);
        }
        return [result.rootIndicatorYearEntryId, ...validPath];
    }, [drillPath, model, result]);

    const openNode = (nodeId: number) => {
        if (!model.childrenByParent.get(nodeId)?.length) return;
        setDrillPath((current) => {
            const existingIndex = current.indexOf(nodeId);
            if (existingIndex >= 0) return current.slice(0, existingIndex + 1);
            return [...current, nodeId];
        });
    };

    const resetTo = (index: number) => {
        if (index <= 0) {
            setDrillPath([]);
            return;
        }
        setDrillPath((current) => current.slice(0, index));
    };

    return (
        <div className="results-stack">
            <AnalyticsViewSelector
                title="Представления аналитики: Иерархия"
                views={SUBTREE_VIEWS}
                enabledSet={enabledViewSet}
                enabledCount={enabledViews.length}
                isDirty={isDirty}
                onToggle={toggleView}
            />

            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Настройки иерархии</h2>
                        <p>Каждый график показывает детей текущего корня. Нажатие на сегмент с потомками добавляет
                            следующий уровень ниже.</p>
                    </div>
                    <div className="status-badges">
                        {result && <span className="status-badge">{result.regionName}</span>}
                    </div>
                </div>
                <div className="view-settings-grid analytics-inline-settings-grid">
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input type="checkbox" checked={showMissing}
                               onChange={(event) => setShowMissing(event.target.checked)}/>
                        <span>Показывать missing-узлы</span>
                    </label>
                    {activeRootIds.length > 1 && (
                        <button type="button" className="secondary-button" onClick={() => setDrillPath([])}>Вернуться к
                            корню</button>
                    )}
                </div>
            </section>

            {enabledViewSet.has('pies') && (
                <Guard loading={loading} error={error} hasData={Boolean(result && result.nodes.length > 0)}
                       emptyMessage="Поддерево ещё не загружено.">
                    {result && (
                        <section className="panel chart-panel result-view-panel">
                            <Header title="Иерархия показателей"
                                    description="На первом уровне доля считается к выбранному корню; на вложенных уровнях дополнительно сохраняется доля к главному корню."/>
                            <div className="drill-breadcrumbs">
                                {activeRootIds.map((rootId, index) => (
                                    <button key={`${rootId}-${index}`} type="button" className="breadcrumb-button"
                                            onClick={() => resetTo(index)}>
                                        {truncateLabel(getNodeTitle(result, model.nodeById.get(rootId)), 54)}
                                    </button>
                                ))}
                            </div>
                            <div className="pie-drill-stack">
                                {activeRootIds.map((rootId, index) => {
                                    const rootNode = model.nodeById.get(rootId);
                                    const children = model.childrenByParent.get(rootId) ?? [];
                                    return (
                                        <article key={`${rootId}-${index}`} className="pie-drill-panel">
                                            <div className="chart-subpanel-header">
                                                <div>
                                                    <h3>{getNodeTitle(result, rootNode)}</h3>
                                                    <p>{children.length > 0 ? 'Показатели на один уровень ниже' : 'У этого узла нет подчиненных показателей'}</p>
                                                </div>
                                                <span>{formatObservationValue(rootNode?.value ?? getRootValue(result))} {result.unitCodeLabel}</span>
                                            </div>
                                            {children.length === 0 ? (
                                                <div className="empty-state compact-empty-state">Нет дочерних узлов для
                                                    следующего графика.</div>
                                            ) : (
                                                <ReactECharts
                                                    style={{height: 460}}
                                                    option={buildPieOption(children, result.unitCodeLabel, index === 0)}
                                                    onEvents={{
                                                        click: (params: { data?: { nodeId?: number } }) => {
                                                            const nodeId = params.data?.nodeId;
                                                            if (nodeId != null) openNode(nodeId);
                                                        }
                                                    }}
                                                    notMerge
                                                />
                                            )}
                                        </article>
                                    );
                                })}
                            </div>
                        </section>
                    )}
                </Guard>
            )}

            {enabledViewSet.has('table') && (
                <section className="panel table-panel result-view-panel">
                    <Header title="Таблица поддерева"
                            description="Контрольная таблица со значением, долей к родителю и долей к главному корню."/>
                    <Guard loading={loading} error={error} hasData={model.nodes.length > 0}
                           emptyMessage="Нет узлов для таблицы.">
                        <div className="result-view-summary-row">
                            <span className="status-badge">Всего строк: {model.nodes.length}</span>
                            <span
                                className="status-badge">В области видимости: {tableVirtualWindow.rows.length} строк</span>
                        </div>
                        <div
                            className="table-wrapper table-wrapper-scroll subtree-table-scroll virtual-table-wrapper"
                            style={{maxHeight: SUBTREE_TABLE_HEIGHT}}
                            onScroll={(event) => setTableScrollTop(event.currentTarget.scrollTop)}
                        >
                            <table>
                                <thead>
                                <tr>
                                    <th>Путь</th>
                                    <th>Уровень</th>
                                    <th>Значение</th>
                                    <th>Доля к родителю</th>
                                    <th>Доля к главному корню</th>
                                    <th>Есть дети</th>
                                    <th>Missing</th>
                                </tr>
                                </thead>
                                <tbody>
                                {tableVirtualWindow.topSpacerHeight > 0 && (
                                    <tr className="virtual-spacer-row" aria-hidden="true">
                                        <td colSpan={7} style={{height: tableVirtualWindow.topSpacerHeight}}/>
                                    </tr>
                                )}
                                {tableVirtualWindow.rows.map((node) => (
                                    <tr key={node.indicatorYearEntryId} style={{height: SUBTREE_TABLE_ROW_HEIGHT}}>
                                        <td><span className="table-cell-clamp" title={node.path}>{node.path}</span></td>
                                        <td>{node.level}</td>
                                        <td>{formatObservationValue(node.value)}</td>
                                        <td>{formatPercentValue(node.shareOfParentPercent)}</td>
                                        <td>{formatPercentValue(node.shareOfRootPercent)}</td>
                                        <td>{node.hasChildren ? 'Да' : 'Нет'}</td>
                                        <td>{node.missing ? 'Да' : 'Нет'}</td>
                                    </tr>
                                ))}
                                {tableVirtualWindow.bottomSpacerHeight > 0 && (
                                    <tr className="virtual-spacer-row" aria-hidden="true">
                                        <td colSpan={7} style={{height: tableVirtualWindow.bottomSpacerHeight}}/>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>
                    </Guard>
                </section>
            )}
        </div>
    );
}

function buildPieOption(children: SubtreeSliceNodeDto[], unitLabel: string, firstLevel: boolean) {
    return {
        color: CHART_COLOR_PALETTE,
        tooltip: buildContainedTooltip({
            trigger: 'item',
            formatter: (params: { data?: PieDatum }) => {
                const data = params.data;
                if (!data) return '';
                const rows: Array<[string, string | number | null | undefined]> = [
                    ['Значение', `${formatObservationValue(data.rawValue)} ${unitLabel}`],
                    ['К текущему корню', formatPercentValue(data.shareOfParentPercent)],
                    ['К главному корню', formatPercentValue(data.shareOfRootPercent)]
                ];
                if (data.hasChildren) {
                    rows.push(['Действие', 'Нажмите, чтобы раскрыть следующий уровень']);
                }
                return buildTooltipHtml(data.fullName, rows);
            }
        }),
        legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 0,
            top: 24,
            bottom: 24,
            width: 260,
            formatter: (value: string) => wrapChartLabel(value, 26, 3),
            textStyle: {lineHeight: 15}
        },
        series: [{
            type: 'pie',
            radius: ['36%', '64%'],
            center: ['34%', '52%'],
            minAngle: 4,
            avoidLabelOverlap: true,
            labelLayout: {hideOverlap: true},
            label: {
                width: 150,
                overflow: 'break',
                lineHeight: 15,
                formatter: (params: { data?: PieDatum }) => {
                    const data = params.data;
                    if (!data) return '';
                    const share = firstLevel ? data.shareOfParentPercent : data.shareOfRootPercent;
                    return `${wrapChartLabel(data.fullName, 18, 2)}\n${formatPercentValue(share)}`;
                }
            },
            data: children.map((node) => ({
                name: truncateLabel(node.indicatorName, 80),
                fullName: node.indicatorName,
                value: Math.max(0, node.value ?? 0),
                rawValue: node.value,
                nodeId: node.indicatorYearEntryId,
                hasChildren: node.hasChildren,
                shareOfParentPercent: node.shareOfParentPercent,
                shareOfRootPercent: node.shareOfRootPercent
            }))
        }]
    };
}

type PieDatum = {
    fullName: string;
    rawValue: number | null;
    nodeId: number;
    hasChildren: boolean;
    shareOfParentPercent: number | null;
    shareOfRootPercent: number | null;
};

function buildVirtualWindow<T>(items: T[], scrollTop: number, rowHeight: number, viewportHeight: number, overscan: number) {
    const visibleCount = Math.ceil(viewportHeight / rowHeight);
    const startIndex = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
    const endIndex = Math.min(items.length, startIndex + visibleCount + overscan * 2);
    const rows = items.slice(startIndex, endIndex);

    return {
        rows,
        topSpacerHeight: startIndex * rowHeight,
        bottomSpacerHeight: Math.max(0, (items.length - endIndex) * rowHeight)
    };
}

function buildSubtreeModel(result: SubtreeSliceResultDto | null, showMissing: boolean) {
    const nodes = (result?.nodes ?? []).filter((node) => showMissing || !node.missing);
    const nodeById = new Map<number, SubtreeSliceNodeDto>();
    const childrenByParent = new Map<number, SubtreeSliceNodeDto[]>();

    for (const node of nodes) {
        nodeById.set(node.indicatorYearEntryId, node);
    }
    for (const node of nodes) {
        if (node.parentIndicatorYearEntryId == null) continue;
        const list = childrenByParent.get(node.parentIndicatorYearEntryId) ?? [];
        list.push(node);
        childrenByParent.set(node.parentIndicatorYearEntryId, list);
    }
    childrenByParent.forEach((list) => list.sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0) || left.indicatorName.localeCompare(right.indicatorName, 'ru')));
    return {nodes, nodeById, childrenByParent};
}

function getNodeTitle(result: SubtreeSliceResultDto, node: SubtreeSliceNodeDto | undefined) {
    return node?.indicatorName ?? result.rootIndicatorName;
}

function getRootValue(result: SubtreeSliceResultDto) {
    return result.nodes.find((node) => node.indicatorYearEntryId === result.rootIndicatorYearEntryId)?.value ?? null;
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
