import type {ReactNode} from 'react';
import {useEffect, useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, formatPercentValue, truncateLabel} from '../../shared/lib/format';
import type {SubtreeSliceNodeDto, SubtreeSliceResultDto} from '../../shared/types/analysis';

export function SubtreeResultPanel({result, loading, error, isDirty}: {
    result: SubtreeSliceResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [drillPath, setDrillPath] = useState<number[]>([]);
    const [showMissing, setShowMissing] = useState(false);

    useEffect(() => {
        setDrillPath([]);
    }, [result?.rootIndicatorYearEntryId]);

    const model = useMemo(() => buildSubtreeModel(result, showMissing), [result, showMissing]);
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
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Поддерево показателя</h2>
                        <p>Каждый пирог показывает детей текущего корня. Нажатие на сегмент с потомками добавляет
                            следующий уровень ниже.</p>
                    </div>
                    <div className="status-badges">
                        {result && <span className="status-badge">{result.regionName}</span>}
                        {isDirty && <span className="warning-badge">Параметры сценария изменены</span>}
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

            <Guard loading={loading} error={error} hasData={Boolean(result && result.nodes.length > 0)}
                   emptyMessage="Поддерево ещё не загружено.">
                {result && (
                    <section className="panel chart-panel result-view-panel">
                        <Header title="Иерархия пирогов"
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
                                                style={{height: 440}}
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

            <section className="panel table-panel result-view-panel">
                <Header title="Таблица поддерева"
                        description="Контрольная таблица со значением, долей к родителю и долей к главному корню."/>
                <Guard loading={loading} error={error} hasData={model.nodes.length > 0}
                       emptyMessage="Нет узлов для таблицы.">
                    <div className="table-wrapper table-wrapper-scroll subtree-table-scroll">
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
                            {model.nodes.map((node) => (
                                <tr key={node.indicatorYearEntryId}>
                                    <td><span className="table-cell-clamp" title={node.path}>{node.path}</span></td>
                                    <td>{node.level}</td>
                                    <td>{formatObservationValue(node.value)}</td>
                                    <td>{formatPercentValue(node.shareOfParentPercent)}</td>
                                    <td>{formatPercentValue(node.shareOfRootPercent)}</td>
                                    <td>{node.hasChildren ? 'Да' : 'Нет'}</td>
                                    <td>{node.missing ? 'Да' : 'Нет'}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </Guard>
            </section>
        </div>
    );
}

function buildPieOption(children: SubtreeSliceNodeDto[], unitLabel: string, firstLevel: boolean) {
    return {
        tooltip: {
            trigger: 'item',
            formatter: (params: { data?: PieDatum }) => {
                const data = params.data;
                if (!data) return '';
                return `${data.fullName}<br/>Значение: ${formatObservationValue(data.rawValue)} ${unitLabel}<br/>Доля к текущему корню: ${formatPercentValue(data.shareOfParentPercent)}<br/>Доля к главному корню: ${formatPercentValue(data.shareOfRootPercent)}${data.hasChildren ? '<br/>Нажмите, чтобы раскрыть следующий уровень' : ''}`;
            }
        },
        legend: {type: 'scroll', orient: 'vertical', right: 0, top: 24, bottom: 24, width: 220},
        series: [{
            type: 'pie',
            radius: ['38%', '68%'],
            center: ['38%', '52%'],
            minAngle: 3,
            avoidLabelOverlap: true,
            label: {
                formatter: (params: { data?: PieDatum }) => {
                    const data = params.data;
                    if (!data) return '';
                    const share = firstLevel ? data.shareOfParentPercent : data.shareOfRootPercent;
                    return `${truncateLabel(data.fullName, 28)}\n${formatPercentValue(share)}`;
                }
            },
            data: children.map((node) => ({
                name: truncateLabel(node.indicatorName, 48),
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
