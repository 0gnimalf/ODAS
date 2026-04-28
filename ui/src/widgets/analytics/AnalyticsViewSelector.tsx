import type {ReactNode} from 'react';

export interface AnalyticsViewDefinition<K extends string> {
    key: K;
    title: string;
    description: string;
}

interface AnalyticsViewSelectorProps<K extends string> {
    title: string;
    description?: string;
    views: Array<AnalyticsViewDefinition<K>>;
    enabledSet: Set<K>;
    enabledCount: number;
    isDirty: boolean;
    onToggle: (key: K) => void;
    extra?: ReactNode;
}

export function AnalyticsViewSelector<K extends string>({
                                                            title,
                                                            description = 'Включайте нужные графики, таблицы и сводки независимо друг от друга.',
                                                            views,
                                                            enabledSet,
                                                            enabledCount,
                                                            isDirty,
                                                            onToggle,
                                                            extra
                                                        }: AnalyticsViewSelectorProps<K>) {
    return (
        <section className="panel result-display-selector-panel">
            <div className="panel-header align-start compact-gap">
                <div>
                    <h2>{title}</h2>
                    <p>{description}</p>
                </div>
                <div className="status-badges">
                    <span className="status-badge">Активно: {enabledCount}</span>
                    {isDirty && <span className="warning-badge">Параметры сценария изменены</span>}
                </div>
            </div>
            <div className="result-view-toggle-grid">
                {views.map((view) => {
                    const active = enabledSet.has(view.key);
                    return (
                        <label key={view.key} className={`result-view-toggle-card ${active ? 'is-enabled' : ''}`}>
                            <div className="result-view-toggle-main">
                                <input type="checkbox" checked={active} onChange={() => onToggle(view.key)}/>
                                <div>
                                    <strong>{view.title}</strong>
                                    <p>{view.description}</p>
                                </div>
                            </div>
                            <span className={`result-view-toggle-status ${active ? 'is-enabled' : ''}`}>
                                {active ? 'Показать' : 'Скрыто'}
                            </span>
                        </label>
                    );
                })}
            </div>
            {extra}
        </section>
    );
}
