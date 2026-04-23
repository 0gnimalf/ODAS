import {useMemo, useState} from 'react';
import {formatRegionLabel} from '../../shared/lib/format';
import type {IndicatorGroupCode, IndicatorGroupReadDto, RegionReadDto} from '../../shared/types/read';

interface FilterPanelProps {
    groups: IndicatorGroupReadDto[];
    regions: RegionReadDto[];
    selectedGroupCode: IndicatorGroupCode | '';
    selectedYear: number;
    selectedMonth: number;
    selectedRegionIds: number[];
    onGroupCodeChange: (value: IndicatorGroupCode | '') => void;
    onYearChange: (value: number) => void;
    onMonthChange: (value: number) => void;
    onRegionIdsChange: (value: number[]) => void;
    onLoadObservations: () => void;
    canLoadObservations: boolean;
    loadingObservations: boolean;
}

const MONTH_OPTIONS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

export function FilterPanel({
                                groups,
                                regions,
                                selectedGroupCode,
                                selectedYear,
                                selectedMonth,
                                selectedRegionIds,
                                onGroupCodeChange,
                                onYearChange,
                                onMonthChange,
                                onRegionIdsChange,
                                onLoadObservations,
                                canLoadObservations,
                                loadingObservations
                            }: FilterPanelProps) {
    const [regionSearch, setRegionSearch] = useState('');

    const filteredRegions = useMemo(() => {
        const query = regionSearch.trim().toLowerCase();
        if (!query) {
            return regions;
        }
        return regions.filter((region) => formatRegionLabel(region).toLowerCase().includes(query));
    }, [regionSearch, regions]);

    const selectedRegionSet = useMemo(() => new Set(selectedRegionIds), [selectedRegionIds]);

    const toggleRegion = (regionId: number) => {
        const next = new Set(selectedRegionIds);
        if (next.has(regionId)) {
            next.delete(regionId);
        } else {
            next.add(regionId);
        }
        onRegionIdsChange(Array.from(next));
    };

    return (
        <section className="panel panel-filters">
            <div className="panel-header">
                <div>
                    <h2>Параметры запроса</h2>
                    <p>Выберите группу, период, регионы и затем загрузите наблюдения.</p>
                </div>
                <button className="primary-button" type="button" onClick={onLoadObservations}
                        disabled={!canLoadObservations || loadingObservations}>
                    {loadingObservations ? 'Загрузка…' : 'Показать данные'}
                </button>
            </div>

            <div className="filters-grid">
                <label className="field">
                    <span>Группа показателей</span>
                    <select value={selectedGroupCode}
                            onChange={(event) => onGroupCodeChange(event.target.value as IndicatorGroupCode | '')}>
                        <option value="">Выберите группу</option>
                        {groups.map((group) => (
                            <option key={group.code} value={group.code}>
                                {group.label}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="field">
                    <span>Год</span>
                    <input
                        type="number"
                        min={2000}
                        max={2100}
                        value={selectedYear}
                        onChange={(event) => onYearChange(Number(event.target.value))}
                    />
                </label>

                <label className="field">
                    <span>Месяц</span>
                    <select value={selectedMonth} onChange={(event) => onMonthChange(Number(event.target.value))}>
                        {MONTH_OPTIONS.map((month) => (
                            <option key={month} value={month}>
                                {month}
                            </option>
                        ))}
                    </select>
                </label>
            </div>

            <div className="region-selector">
                <div className="selector-header-row">
                    <strong>Регионы</strong>
                    <span>{selectedRegionIds.length} выбрано</span>
                </div>

                <div className="selector-actions">
                    <input
                        type="search"
                        value={regionSearch}
                        onChange={(event) => setRegionSearch(event.target.value)}
                        placeholder="Поиск региона"
                    />
                    <div className="inline-actions">
                        <button
                            type="button"
                            onClick={() => onRegionIdsChange(Array.from(new Set([...selectedRegionIds, ...filteredRegions.map((region) => region.id)])))}
                        >
                            Выбрать видимые
                        </button>
                        <button type="button" onClick={() => onRegionIdsChange([])}>
                            Очистить
                        </button>
                    </div>
                </div>

                <div className="region-list">
                    {filteredRegions.map((region) => (
                        <label key={region.id} className="check-row">
                            <input
                                type="checkbox"
                                checked={selectedRegionSet.has(region.id)}
                                onChange={() => toggleRegion(region.id)}
                            />
                            <span>{formatRegionLabel(region)}</span>
                        </label>
                    ))}
                    {filteredRegions.length === 0 &&
                        <div className="empty-state compact">По запросу ничего не найдено.</div>}
                </div>
            </div>
        </section>
    );
}
