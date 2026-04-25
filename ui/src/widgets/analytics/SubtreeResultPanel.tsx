import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, formatPercentValue, truncateLabel} from '../../shared/lib/format';
import type {SubtreeSliceNodeDto, SubtreeSliceResultDto} from '../../shared/types/analysis';

type ViewKey = 'treemap' | 'table' | 'shares' | 'structure';

const VIEWS: Array<{ key: ViewKey; title: string; description: string }> = [
    {key: 'treemap', title: 'Treemap', description: 'Визуализация структуры поддерева по размерам узлов.'},
    {key: 'table', title: 'Иерархическая таблица', description: 'Путь, уровень, значение и доли.'},
    {key: 'shares', title: 'Доли узлов', description: 'Сравнение вкладов первого уровня.'},
    {key: 'structure', title: 'Структурный список', description: 'Компактный древовидный список.'}
];

export function SubtreeResultPanel({result, loading, error, isDirty}: {
    result: SubtreeSliceResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [enabled, setEnabled] = useState<ViewKey[]>(['treemap', 'table', 'structure']);
    const [showOnlyLeaves, setShowOnlyLeaves] = useState(false);
    const [shareMode, setShareMode] = useState<'root' | 'parent'>('root');

    const enabledSet = useMemo(() => new Set(enabled), [enabled]);
    const nodes = useMemo(() => (result?.nodes ?? []).filter((node) => !showOnlyLeaves || !node.hasChildren), [result, showOnlyLeaves]);
    const topChildren = useMemo(() => nodes.filter((node) => node.level === 1), [nodes]);

    const toggle = (key: ViewKey) => {
        setEnabled((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key]);
    };

    return (
        <div className="results-stack">
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Представления аналитики: поддерево</h2>
                        <p>Treemap, таблица, доли узлов и структурный список с независимым включением.</p>
                    </div>
                    <div className="status-badges">
                        <span className="status-badge">Активно: {enabled.length}</span>
                        {isDirty && <span className="warning-badge">Параметры сценария изменены</span>}
                    </div>
                </div>

                <div className="view-settings-grid analytics-inline-settings-grid">
                    <label className="field field-fit-content">
                        <span>Режим доли</span>
                        <select value={shareMode}
                                onChange={(event) => setShareMode(event.target.value as 'root' | 'parent')}>
                            <option value="root">К корню</option>
                            <option value="parent">К родителю</option>
                        </select>
                    </label>
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input type="checkbox" checked={showOnlyLeaves}
                               onChange={(event) => setShowOnlyLeaves(event.target.checked)}/>
                        <span>Только листья</span>
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

            {enabledSet.has('treemap') && (
                <section className="panel chart-panel result-view-panel">
                    <Header title="Treemap" description="Структура поддерева по выбранному корню."/>
                    <Guard loading={loading} error={error} hasData={Boolean(result?.nodes.length)}
                           emptyMessage="Поддерево ещё не загружено.">
                        <ReactECharts
                            style={{height: 520}}
                            option={{
                                tooltip: {
                                    formatter: (params: {
                                        data: { label?: string; value?: number }
                                    }) => `${params.data.label ?? ''}<br/>${formatObservationValue(params.data.value ?? null)}`
                                },
                                series: [{
                                    type: 'treemap',
                                    breadcrumb: {show: true},
                                    label: {show: true, formatter: '{b}'},
                                    upperLabel: {show: true},
                                    data: buildTreemap(result?.nodes ?? [], result?.rootIndicatorYearEntryId ?? 0)
                                }]
                            }}
                        />
                    </Guard>
                </section>
            )}

            {enabledSet.has('shares') && (
                <section className="panel chart-panel result-view-panel">
                    <Header title="Доли узлов" description="Сравнение вкладов первого уровня внутри выбранного корня."/>
                    <Guard loading={loading} error={error} hasData={topChildren.length > 0}
                           emptyMessage="Нет узлов первого уровня для графика долей.">
                        <ReactECharts
                            style={{height: Math.max(320, topChildren.length * 32)}}
                            option={{
                                tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
                                grid: {left: 240, right: 24, top: 24, bottom: 24},
                                xAxis: {type: 'value'},
                                yAxis: {
                                    type: 'category',
                                    data: topChildren.map((node) => node.indicatorName),
                                    axisLabel: {formatter: (value: string) => truncateLabel(value, 28)}
                                },
                                series: [{
                                    type: 'bar',
                                    label: {
                                        show: true,
                                        position: 'right',
                                        formatter: ({value}: { value: number }) => formatPercentValue(value)
                                    },
                                    data: topChildren.map((node) => shareMode === 'root' ? node.shareOfRootPercent : node.shareOfParentPercent)
                                }]
                            }}
                        />
                    </Guard>
                </section>
            )}

            {enabledSet.has('table') && (
                <section className="panel table-panel result-view-panel">
                    <Header title="Иерархическая таблица"
                            description="Путь, уровень, значение и доли по каждому узлу поддерева."/>
                    <Guard loading={loading} error={error} hasData={nodes.length > 0}
                           emptyMessage="Нет узлов для отображения.">
                        <div className="table-wrapper">
                            <table>
                                <thead>
                                <tr>
                                    <th>Путь</th>
                                    <th>Уровень</th>
                                    <th>Значение</th>
                                    <th>Доля к родителю</th>
                                    <th>Доля к корню</th>
                                    <th>Missing</th>
                                </tr>
                                </thead>
                                <tbody>
                                {nodes.map((node) => (
                                    <tr key={node.indicatorYearEntryId}>
                                        <td>
                                            <span className="table-cell-clamp" title={node.path}>{node.path}</span>
                                        </td>
                                        <td>{node.level}</td>
                                        <td>{formatObservationValue(node.value)}</td>
                                        <td>{formatPercentValue(node.shareOfParentPercent)}</td>
                                        <td>{formatPercentValue(node.shareOfRootPercent)}</td>
                                        <td>{node.missing ? 'Да' : 'Нет'}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </Guard>
                </section>
            )}

            {enabledSet.has('structure') && (
                <section className="panel result-view-panel">
                    <Header title="Структурный список"
                            description="Компактный древовидный список для точного просмотра структуры."/>
                    <Guard loading={loading} error={error} hasData={nodes.length > 0}
                           emptyMessage="Структурный список пуст.">
                        <div className="analytics-structure-list">
                            {nodes.map((node) => (
                                <article key={node.indicatorYearEntryId} className="analytics-structure-item"
                                         style={{paddingLeft: `${node.level * 20 + 12}px`}}>
                                    <div>
                                        <strong
                                            title={node.indicatorName}>{truncateLabel(node.indicatorName, 64)}</strong>
                                        <div className="analytics-structure-meta" title={node.path}>{node.path}</div>
                                    </div>
                                    <div className="analytics-structure-values">
                                        <span>{formatObservationValue(node.value)}</span>
                                        <span>{formatPercentValue(shareMode === 'root' ? node.shareOfRootPercent : node.shareOfParentPercent)}</span>
                                    </div>
                                </article>
                            ))}
                        </div>
                    </Guard>
                </section>
            )}
        </div>
    );
}

function buildTreemap(nodes: SubtreeSliceNodeDto[], rootId: number) {
    const childrenByParent = new Map<number | null, SubtreeSliceNodeDto[]>();

    nodes.forEach((node) => {
        const key = node.parentIndicatorYearEntryId ?? null;
        const list = childrenByParent.get(key) ?? [];
        list.push(node);
        childrenByParent.set(key, list);
    });

    const visit = (parentId: number): Array<Record<string, unknown>> =>
        (childrenByParent.get(parentId) ?? []).map((item) => ({
            name: item.indicatorName,
            label: item.indicatorName,
            value: item.value ?? 0,
            children: visit(item.indicatorYearEntryId)
        }));

    return visit(rootId);
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
