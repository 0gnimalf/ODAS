import type {ReactNode} from 'react';
import {useMemo, useState} from 'react';
import ReactECharts from 'echarts-for-react';
import {formatObservationValue, formatPercentValue} from '../../shared/lib/format';
import type {RegionComparisonResultDto} from '../../shared/types/analysis';

type ViewKey = 'ranking' | 'shares' | 'distribution' | 'table';
const VIEWS: Array<{ key: ViewKey; title: string; description: string }> = [
    {key: 'ranking', title: 'Рейтинг регионов', description: 'Горизонтальная гистограмма по выбранному показателю.'},
    {key: 'shares', title: 'Доли в общем объёме', description: 'Вклад каждого региона в общую сумму.'},
    {key: 'distribution', title: 'Сводка распределения', description: 'Мин/макс/среднее/медиана и покрытие выборки.'},
    {key: 'table', title: 'Таблица сравнения', description: 'Регион, значение, ранг и отклонения.'}
];

export function ComparisonResultPanel({result, loading, error, isDirty}: {
    result: RegionComparisonResultDto | null;
    loading: boolean;
    error: string | null;
    isDirty: boolean;
}) {
    const [enabled, setEnabled] = useState<ViewKey[]>(['ranking', 'distribution', 'table']);
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
    const [maxRegions, setMaxRegions] = useState(15);
    const [hideMissing, setHideMissing] = useState(true);
    const [filter, setFilter] = useState('');
    const enabledSet = useMemo(() => new Set(enabled), [enabled]);
    const items = useMemo(() => {
        const next = (result?.items ?? []).filter((item) => (!hideMissing || !item.missing) && (!filter || item.regionName.toLowerCase().includes(filter.toLowerCase())));
        next.sort((a, b) => (sortDirection === 'desc' ? (b.value ?? -Infinity) - (a.value ?? -Infinity) : (a.value ?? -Infinity) - (b.value ?? -Infinity)));
        return maxRegions > 0 ? next.slice(0, maxRegions) : next;
    }, [result, hideMissing, filter, sortDirection, maxRegions]);
    const toggle = (key: ViewKey) => setEnabled((current) => current.includes(key) ? current.filter((item) => item !== key) : [...current, key]);

    return <div className="results-stack">
        <section className="panel result-display-selector-panel">
            <div className="panel-header align-start compact-gap">
                <div><h2>Представления аналитики: сравнение регионов</h2><p>Независимые режимы для рейтинга, долей,
                    статистической сводки и подробной таблицы.</p></div>
                <div className="status-badges"><span
                    className="status-badge">Активно: {enabled.length}</span>{isDirty &&
                    <span className="warning-badge">Параметры сценария изменены</span>}</div>
            </div>
            <div className="view-settings-grid analytics-inline-settings-grid"><label
                className="field field-fit-content"><span>Порядок рейтинга</span><select value={sortDirection}
                                                                                         onChange={(e) => setSortDirection(e.target.value as 'asc' | 'desc')}>
                <option value="desc">По убыванию</option>
                <option value="asc">По возрастанию</option>
            </select></label><label className="field field-fit-content"><span>Макс. регионов</span><input type="number"
                                                                                                          min={0}
                                                                                                          max={200}
                                                                                                          value={maxRegions}
                                                                                                          onChange={(e) => setMaxRegions(Math.max(0, Number(e.target.value) || 0))}/></label><label
                className="field"><span>Фильтр по региону</span><input type="search" value={filter}
                                                                       onChange={(e) => setFilter(e.target.value)}
                                                                       placeholder="Название региона"/></label><label
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
        {enabledSet.has('ranking') &&
            <section className="panel chart-panel result-view-panel"><Header title="Рейтинг регионов"
                                                                             description="Горизонтальная гистограмма для выбранного показателя."/><Guard
                loading={loading} error={error} hasData={items.length > 0}
                emptyMessage="Нет регионов для отображения рейтинга."><ReactECharts
                style={{height: Math.max(320, items.length * 32)}} option={{
                tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
                grid: {left: 220, right: 24, top: 24, bottom: 24},
                xAxis: {type: 'value'},
                yAxis: {type: 'category', data: items.map((item) => item.regionName)},
                series: [{
                    type: 'bar',
                    label: {
                        show: true,
                        position: 'right',
                        formatter: ({value}: { value: number }) => formatObservationValue(value)
                    },
                    data: items.map((item) => item.value)
                }]
            }}/></Guard></section>}
        {enabledSet.has('shares') &&
            <section className="panel chart-panel result-view-panel"><Header title="Доли регионов"
                                                                             description="Сравнение вкладов регионов в общую сумму."/><Guard
                loading={loading} error={error} hasData={items.length > 0}
                emptyMessage="Нет данных по долям."><ReactECharts style={{height: Math.max(320, items.length * 28)}}
                                                                  option={{
                                                                      tooltip: {
                                                                          trigger: 'axis',
                                                                          axisPointer: {type: 'shadow'}
                                                                      },
                                                                      grid: {left: 220, right: 24, top: 24, bottom: 24},
                                                                      xAxis: {type: 'value'},
                                                                      yAxis: {
                                                                          type: 'category',
                                                                          data: items.map((item) => item.regionName)
                                                                      },
                                                                      series: [{
                                                                          type: 'bar',
                                                                          label: {
                                                                              show: true,
                                                                              position: 'right',
                                                                              formatter: ({value}: {
                                                                                  value: number
                                                                              }) => formatPercentValue(value)
                                                                          },
                                                                          data: items.map((item) => item.shareOfTotalPercent)
                                                                      }]
                                                                  }}/></Guard></section>}
        {enabledSet.has('distribution') &&
            <section className="panel result-view-panel"><Header title="Сводка распределения"
                                                                 description="Ключевые статистики по найденной выборке регионов."/><Guard
                loading={loading} error={error} hasData={Boolean(result)}
                emptyMessage="Сводка распределения недоступна.">
                <div className="analytics-card-grid metrics-grid-compact"><Card title="Запрошено регионов"
                                                                                value={result?.summary.requestedRegionCount}/><Card
                    title="Найдено значений" value={result?.summary.foundRegionCount}/><Card title="Минимум"
                                                                                             value={result?.summary.minValue}/><Card
                    title="Максимум" value={result?.summary.maxValue}/><Card title="Среднее"
                                                                             value={result?.summary.averageValue}/><Card
                    title="Медиана" value={result?.summary.medianValue}/><Card title="Сумма"
                                                                               value={result?.summary.totalValue}/>
                </div>
            </Guard></section>}
        {enabledSet.has('table') &&
            <section className="panel table-panel result-view-panel"><Header title="Таблица сравнения"
                                                                             description="Регион, значение, ранг и отклонения от лидера и среднего."/><Guard
                loading={loading} error={error} hasData={items.length > 0} emptyMessage="Таблица сравнения пуста.">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Регион</th>
                            <th>Значение</th>
                            <th>Missing</th>
                            <th>Ранг</th>
                            <th>Доля</th>
                            <th>Δ от лидера</th>
                            <th>Δ от среднего</th>
                        </tr>
                        </thead>
                        <tbody>{items.map((item) => <tr key={item.regionId}>
                            <td>{item.regionName}</td>
                            <td>{formatObservationValue(item.value)}</td>
                            <td>{item.missing ? 'Да' : 'Нет'}</td>
                            <td>{item.rank ?? '—'}</td>
                            <td>{formatPercentValue(item.shareOfTotalPercent)}</td>
                            <td>{formatObservationValue(item.deltaFromLeader)}</td>
                            <td>{formatObservationValue(item.deltaFromAverage)}</td>
                        </tr>)}</tbody>
                    </table>
                </div>
            </Guard></section>}
    </div>;
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

function Card({title, value}: { title: string; value: number | null | undefined }) {
    return <article className="analytics-kpi-card"><span className="analytics-kpi-title">{title}</span><strong
        className="analytics-kpi-value">{typeof value === 'number' ? formatObservationValue(value) : value ?? '—'}</strong>
    </article>;
}
