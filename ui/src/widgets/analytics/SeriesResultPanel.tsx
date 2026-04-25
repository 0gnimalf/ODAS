import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, formatPercentValue} from '../../shared/lib/format';
import type {MonthlySeriesResultDto, PeriodGrowthMetricsResultDto} from '../../shared/types/analysis';

type ViewKey = 'cumulative' | 'nonCumulative' | 'quarters' | 'growth' | 'seriesTable' | 'quarterTable' | 'tech';
const VIEWS: Array<{ key: ViewKey; title: string; description: string }> = [
    {key: 'cumulative', title: 'Линейный график накопленных значений', description: 'Исходный ряд по месяцам.'},
    {key: 'nonCumulative', title: 'График чистых ненакопленных значений', description: 'Помесячные чистые значения.'},
    {key: 'quarters', title: 'Квартальные агрегаты', description: 'Суммы по кварталам.'},
    {key: 'growth', title: 'Метрики роста периода', description: 'Карточки месяца и квартала.'},
    {key: 'seriesTable', title: 'Таблица ряда', description: 'Месячные точки ряда.'},
    {key: 'quarterTable', title: 'Таблица кварталов', description: 'Подробные квартальные значения.'},
    {key: 'tech', title: 'Техническая сводка расчёта', description: 'Покрытие и режим расчёта.'}
];

export function SeriesResultPanel({seriesResult, growthResult, loading, error, isDirty}: {
    seriesResult: MonthlySeriesResultDto | null;
    growthResult: PeriodGrowthMetricsResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [enabled, setEnabled] = useState<ViewKey[]>(['cumulative', 'nonCumulative', 'growth', 'seriesTable']);
    const [showLabels, setShowLabels] = useState(true);
    const [showIncompleteQuarters, setShowIncompleteQuarters] = useState(true);
    const [search, setSearch] = useState('');
    const enabledSet = useMemo(() => new Set(enabled), [enabled]);
    const seriesRows = useMemo(() => (seriesResult?.points ?? []).filter((p) => !search || p.periodLabel.toLowerCase().includes(search.toLowerCase())), [seriesResult, search]);
    const quarterRows = useMemo(() => (seriesResult?.quarterAggregates ?? []).filter((q) => showIncompleteQuarters || q.complete), [seriesResult, showIncompleteQuarters]);
    const toggle = (key: ViewKey) => setEnabled((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key]);

    return <div className="results-stack">
        <ViewSelector title="Представления аналитики: ряд и темпы" enabledCount={enabled.length} isDirty={isDirty}
                      views={VIEWS} enabledSet={enabledSet} onToggle={toggle}/>
        {enabledSet.has('cumulative') && <section className="panel chart-panel result-view-panel">
            <SectionHeader title="Линейный график накопленных значений"
                           description="Исходный накопленный ряд по всем доступным месяцам."
                           extra={<label className="check-row checkbox-card compact-checkbox-card"><input
                               type="checkbox" checked={showLabels}
                               onChange={(e) => setShowLabels(e.target.checked)}/><span>Подписи точек</span></label>}/>
            <Guard loading={loading} error={error} hasData={Boolean(seriesResult?.points.length)}
                   emptyMessage="Ряд ещё не загружен.">
                <ReactECharts style={{height: 420}} option={{
                    tooltip: {trigger: 'axis'},
                    grid: {left: 56, right: 24, top: 24, bottom: 48},
                    xAxis: {type: 'category', data: seriesResult?.points.map((p) => p.periodLabel) ?? []},
                    yAxis: {type: 'value'},
                    series: [{
                        type: 'line',
                        label: showLabels ? {
                            show: true,
                            formatter: ({value}: { value: number }) => formatObservationValue(value)
                        } : undefined,
                        data: seriesResult?.points.map((p) => p.cumulativeValue) ?? []
                    }]
                }}/>
            </Guard>
        </section>}
        {enabledSet.has('nonCumulative') && <section className="panel chart-panel result-view-panel">
            <SectionHeader title="Чистые ненакопленные значения"
                           description="Гистограмма по чистым значениям для абсолютных рядов."/>
            <Guard loading={loading} error={error} hasData={Boolean(seriesResult?.points.length)}
                   emptyMessage="Ряд ещё не загружен.">
                <ReactECharts style={{height: 380}} option={{
                    tooltip: {trigger: 'axis'},
                    grid: {left: 56, right: 24, top: 24, bottom: 48},
                    xAxis: {type: 'category', data: seriesResult?.points.map((p) => p.periodLabel) ?? []},
                    yAxis: {type: 'value'},
                    series: [{
                        type: 'bar',
                        label: showLabels ? {
                            show: true,
                            position: 'top',
                            formatter: ({value}: { value: number }) => formatObservationValue(value)
                        } : undefined,
                        data: seriesResult?.points.map((p) => p.nonCumulativeValue) ?? []
                    }]
                }}/>
            </Guard>
        </section>}
        {enabledSet.has('quarters') && <section className="panel chart-panel result-view-panel">
            <SectionHeader title="Квартальные агрегаты" description="Агрегация на уровне кварталов."
                           extra={<label className="check-row checkbox-card compact-checkbox-card"><input
                               type="checkbox" checked={showIncompleteQuarters}
                               onChange={(e) => setShowIncompleteQuarters(e.target.checked)}/><span>Показывать неполные кварталы</span></label>}/>
            <Guard loading={loading} error={error} hasData={quarterRows.length > 0}
                   emptyMessage="Квартальные агрегаты недоступны.">
                <ReactECharts style={{height: 360}} option={{
                    tooltip: {trigger: 'axis'},
                    grid: {left: 56, right: 24, top: 24, bottom: 48},
                    xAxis: {type: 'category', data: quarterRows.map((q) => q.label)},
                    yAxis: {type: 'value'},
                    series: [{
                        type: 'bar',
                        label: {
                            show: true,
                            position: 'top',
                            formatter: ({value}: { value: number }) => formatObservationValue(value)
                        },
                        data: quarterRows.map((q) => q.aggregatedValue)
                    }]
                }}/>
            </Guard>
        </section>}
        {enabledSet.has('growth') && <section className="panel result-view-panel">
            <SectionHeader title="Метрики роста периода"
                           description="Карточки для целевого месяца, сравнительных точек и кварталов."/>
            <Guard loading={loading} error={error} hasData={Boolean(growthResult)}
                   emptyMessage="Метрики роста ещё не загружены.">
                <div className="analytics-card-grid">
                    <Kpi title="Целевой месяц"
                         value={growthResult?.targetMonthPoint?.nonCumulativeValue ?? growthResult?.targetMonthPoint?.cumulativeValue}
                         subtitle={growthResult?.targetMonthPoint?.periodLabel ?? '—'}/>
                    <Kpi title="Предыдущий месяц"
                         value={growthResult?.previousMonthPoint?.nonCumulativeValue ?? growthResult?.previousMonthPoint?.cumulativeValue}
                         subtitle={growthResult?.previousMonthPoint?.periodLabel ?? '—'}/>
                    <Kpi title="Тот же месяц прошлого года"
                         value={growthResult?.sameMonthPreviousYearPoint?.nonCumulativeValue ?? growthResult?.sameMonthPreviousYearPoint?.cumulativeValue}
                         subtitle={growthResult?.sameMonthPreviousYearPoint?.periodLabel ?? '—'}/>
                    <Kpi title="Текущий квартал" value={growthResult?.currentQuarter?.aggregatedValue}
                         subtitle={growthResult?.currentQuarter?.label ?? '—'}/>
                    <Kpi title="Предыдущий квартал" value={growthResult?.previousQuarter?.aggregatedValue}
                         subtitle={growthResult?.previousQuarter?.label ?? '—'}/>
                    <Kpi title="Тот же квартал прошлого года"
                         value={growthResult?.sameQuarterPreviousYear?.aggregatedValue}
                         subtitle={growthResult?.sameQuarterPreviousYear?.label ?? '—'}/>
                </div>
                <div className="analytics-card-grid metrics-grid-compact top-margin-16">
                    <Kpi title="Δ к предыдущему месяцу" value={growthResult?.absoluteDeltaToPreviousMonth}
                         subtitle={formatPercentValue(growthResult?.rateToPreviousMonthPercent)}/>
                    <Kpi title="Δ к тому же месяцу прошлого года"
                         value={growthResult?.absoluteDeltaToSameMonthPreviousYear}
                         subtitle={formatPercentValue(growthResult?.rateToSameMonthPreviousYearPercent)}/>
                    <Kpi title="Δ к предыдущему кварталу" value={growthResult?.absoluteDeltaToPreviousQuarter}
                         subtitle={formatPercentValue(growthResult?.rateToPreviousQuarterPercent)}/>
                    <Kpi title="Δ к тому же кварталу прошлого года"
                         value={growthResult?.absoluteDeltaToSameQuarterPreviousYear}
                         subtitle={formatPercentValue(growthResult?.rateToSameQuarterPreviousYearPercent)}/>
                </div>
            </Guard>
        </section>}
        {enabledSet.has('seriesTable') && <section className="panel table-panel result-view-panel">
            <SectionHeader title="Таблица ряда"
                           description="Подробные месячные точки с накопленными и чистыми значениями."
                           extra={<label className="field compact-search-field"><span>Поиск по периоду</span><input
                               type="search" value={search} onChange={(e) => setSearch(e.target.value)}
                               placeholder="Март 2025"/></label>}/>
            <Guard loading={loading} error={error} hasData={seriesRows.length > 0}
                   emptyMessage="Нет строк для отображения.">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Период</th>
                            <th>Накопленное</th>
                            <th>Чистое</th>
                            <th>Рассчитано</th>
                            <th>Аномалия</th>
                        </tr>
                        </thead>
                        <tbody>{seriesRows.map((p) => <tr key={`${p.year}-${p.month}`}>
                            <td>{p.periodLabel}</td>
                            <td>{formatObservationValue(p.cumulativeValue)}</td>
                            <td>{formatObservationValue(p.nonCumulativeValue)}</td>
                            <td>{p.nonCumulativeCalculated ? 'Да' : 'Нет'}</td>
                            <td>{p.anomaly ? 'Да' : '—'}</td>
                        </tr>)}</tbody>
                    </table>
                </div>
            </Guard>
        </section>}
        {enabledSet.has('quarterTable') && <section className="panel table-panel result-view-panel">
            <SectionHeader title="Таблица кварталов" description="Структурированный список квартальных агрегатов."/>
            <Guard loading={loading} error={error} hasData={quarterRows.length > 0}
                   emptyMessage="Квартальные строки отсутствуют.">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Год</th>
                            <th>Квартал</th>
                            <th>Метка</th>
                            <th>Значение</th>
                            <th>Покрытие</th>
                            <th>Полный квартал</th>
                        </tr>
                        </thead>
                        <tbody>{quarterRows.map((q) => <tr key={`${q.year}-${q.quarter}`}>
                            <td>{q.year}</td>
                            <td>{q.quarter}</td>
                            <td>{q.label}</td>
                            <td>{formatObservationValue(q.aggregatedValue)}</td>
                            <td>{q.coveredMonthCount} / 3</td>
                            <td>{q.complete ? 'Да' : 'Нет'}</td>
                        </tr>)}</tbody>
                    </table>
                </div>
            </Guard>
        </section>}
        {enabledSet.has('tech') && <section className="panel result-view-panel">
            <SectionHeader title="Техническая сводка расчёта"
                           description="Служебная информация о покрытии ряда и дозагрузке."/>
            <Guard loading={loading} error={error} hasData={Boolean(seriesResult)}
                   emptyMessage="Сводка ещё не сформирована.">
                <div className="analytics-card-grid metrics-grid-compact">
                    <Kpi title="Ожидаемых месяцев" value={seriesResult?.expectedMonthCount ?? null}
                         subtitle="Для расчётного диапазона"/>
                    <Kpi title="Доступных месяцев" value={seriesResult?.availableMonthCount ?? null}
                         subtitle="После чтения и дозагрузки"/>
                    <Kpi title="Режим ненакопленных"
                         value={seriesResult?.nonCumulativeMode === 'SERIES_RANGE' ? 'Ряд' : 'Метрики периода'}
                         subtitle={seriesResult?.nonCumulativeMode ?? ''}/>
                    <Kpi title="Автосбор недостающих" value={seriesResult?.autoCollectedMissing ? 'Да' : 'Нет'}
                         subtitle={seriesResult?.indicatorName ?? ''}/>
                </div>
            </Guard>
        </section>}
    </div>;
}

function ViewSelector({title, enabledCount, isDirty, views, enabledSet, onToggle}: {
    title: string;
    enabledCount: number;
    isDirty: boolean;
    views: Array<{ key: ViewKey; title: string; description: string }>;
    enabledSet: Set<ViewKey>;
    onToggle: (key: ViewKey) => void;
}) {
    return <section className="panel result-display-selector-panel">
        <div className="panel-header align-start compact-gap">
            <div><h2>{title}</h2><p>Включайте нужные графики и таблицы независимо друг от друга.</p></div>
            <div className="status-badges"><span className="status-badge">Активно: {enabledCount}</span>{isDirty &&
                <span className="warning-badge">Параметры сценария изменены</span>}</div>
        </div>
        <div className="result-view-toggle-grid">{views.map((view) => {
            const active = enabledSet.has(view.key);
            return <label key={view.key} className={`result-view-toggle-card ${active ? 'is-enabled' : ''}`}>
                <div className="result-view-toggle-main"><input type="checkbox" checked={active}
                                                                onChange={() => onToggle(view.key)}/>
                    <div><strong>{view.title}</strong><p>{view.description}</p></div>
                </div>
                <span
                    className={`result-view-toggle-status ${active ? 'is-enabled' : ''}`}>{active ? 'Показать' : 'Скрыто'}</span></label>;
        })}</div>
    </section>;
}

function SectionHeader({title, description, extra}: { title: string; description: string; extra?: ReactNode }) {
    return <div className="panel-header align-start compact-gap">
        <div><h2>{title}</h2><p>{description}</p></div>
        {extra}</div>;
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

function Kpi({title, value, subtitle}: { title: string; value: number | string | null | undefined; subtitle: string }) {
    return <article className="analytics-kpi-card"><span className="analytics-kpi-title">{title}</span><strong
        className="analytics-kpi-value">{typeof value === 'number' ? formatObservationValue(value) : value ?? '—'}</strong><span
        className="analytics-kpi-subtitle">{subtitle}</span></article>;
}
