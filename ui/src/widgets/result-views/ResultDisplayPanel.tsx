import {useMemo, useState} from 'react';
import {RegionCompareBarChart} from '../charts/RegionCompareBarChart';
import {ObservationTable} from '../observation-table/ObservationTable';
import type {ObservationReadResultDto} from '../../shared/types/read';

type ResultViewKey = 'regionCompareBar' | 'table';

interface ResultDisplayPanelProps {
    result: ObservationReadResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
    regionChartAvailable: boolean;
    regionChartUnavailableReason: string;
}

interface ResultViewDefinition {
    key: ResultViewKey;
    title: string;
    description: string;
}

const RESULT_VIEW_DEFINITIONS: ResultViewDefinition[] = [
    {
        key: 'regionCompareBar',
        title: 'Сравнение регионов',
        description: 'Гистограмма для сравнения регионов по одному выбранному показателю.'
    },
    {
        key: 'table',
        title: 'Таблица наблюдений',
        description: 'Табличное представление с настройкой колонок, фильтров и сортировки.'
    }
];

const DEFAULT_ENABLED_VIEWS: ResultViewKey[] = ['regionCompareBar', 'table'];

export function ResultDisplayPanel({
                                       result,
                                       loading,
                                       error,
                                       isDirty,
                                       regionChartAvailable,
                                       regionChartUnavailableReason
                                   }: ResultDisplayPanelProps) {
    const [enabledViewKeys, setEnabledViewKeys] = useState<ResultViewKey[]>(DEFAULT_ENABLED_VIEWS);

    const enabledViewKeySet = useMemo(() => new Set(enabledViewKeys), [enabledViewKeys]);

    const toggleView = (viewKey: ResultViewKey) => {
        if (viewKey === 'regionCompareBar' && !regionChartAvailable) return;
        setEnabledViewKeys((current) => {
            if (current.includes(viewKey)) {
                return current.filter((item) => item !== viewKey);
            }
            return [...current, viewKey];
        });
    };

    return (
        <div className="results-stack">
            <section className="panel result-display-selector-panel">
                <div className="panel-header align-start compact-gap">
                    <div>
                        <h2>Виды отображения результата</h2>
                        <p>Включайте нужные представления независимо друг от друга. У каждого блока ниже свои
                            настройки.</p>
                    </div>
                    <div className="status-badges">
                        <span
                            className="status-badge">Активно: {enabledViewKeys.filter((key) => key !== 'regionCompareBar' || regionChartAvailable).length}</span>
                        {isDirty && <span className="warning-badge">Фильтры изменены</span>}
                    </div>
                </div>

                <div className="result-view-toggle-grid">
                    {RESULT_VIEW_DEFINITIONS.map((view) => {
                        const unavailable = view.key === 'regionCompareBar' && !regionChartAvailable;
                        const enabled = enabledViewKeySet.has(view.key) && !unavailable;
                        return (
                            <label key={view.key}
                                   className={`result-view-toggle-card ${enabled ? 'is-enabled' : ''} ${unavailable ? 'is-disabled' : ''}`}
                                   title={unavailable ? regionChartUnavailableReason : undefined}>
                                <div className="result-view-toggle-main">
                                    <input
                                        type="checkbox"
                                        checked={enabled}
                                        disabled={unavailable}
                                        onChange={() => toggleView(view.key)}
                                    />
                                    <div>
                                        <strong>{view.title}</strong>
                                        <p>{unavailable ? regionChartUnavailableReason : view.description}</p>
                                    </div>
                                </div>
                                <span className={`result-view-toggle-status ${enabled ? 'is-enabled' : ''}`}>
                                    {unavailable ? 'Недоступно' : enabled ? 'Показать' : 'Скрыто'}
                                </span>
                            </label>
                        );
                    })}
                </div>
            </section>

            {regionChartAvailable && enabledViewKeySet.has('regionCompareBar') && (
                <RegionCompareBarChart result={result} loading={loading} error={error} isDirty={isDirty}/>
            )}

            {enabledViewKeySet.has('table') && (
                <ObservationTable result={result} loading={loading} error={error} isDirty={isDirty}/>
            )}
        </div>
    );
}
