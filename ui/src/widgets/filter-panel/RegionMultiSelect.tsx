import {useEffect, useMemo, useRef, useState} from 'react';
import {formatRegionLabel} from '../../shared/lib/format';
import type {RegionReadDto} from '../../shared/types/read';

interface RegionMultiSelectProps {
    regions: RegionReadDto[];
    selectedRegionIds: number[];
    onChange: (value: number[]) => void;
    disabled?: boolean;
}

export function RegionMultiSelect({regions, selectedRegionIds, onChange, disabled = false}: RegionMultiSelectProps) {
    const rootRef = useRef<HTMLDivElement | null>(null);
    const searchInputRef = useRef<HTMLInputElement | null>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [search, setSearch] = useState('');

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const handlePointerDown = (event: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [isOpen]);

    useEffect(() => {
        if (isOpen) {
            searchInputRef.current?.focus();
        }
    }, [isOpen]);

    const selectedIdSet = useMemo(() => new Set(selectedRegionIds), [selectedRegionIds]);
    const selectedRegions = useMemo(
        () => regions.filter((region) => selectedIdSet.has(region.id)),
        [regions, selectedIdSet]
    );
    const filteredRegions = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) {
            return regions;
        }
        return regions.filter((region) => formatRegionLabel(region).toLowerCase().includes(query));
    }, [regions, search]);

    const toggleRegion = (regionId: number) => {
        const next = new Set(selectedRegionIds);
        if (next.has(regionId)) {
            next.delete(regionId);
        } else {
            next.add(regionId);
        }
        onChange(Array.from(next));
    };

    const handleSelectVisible = () => {
        onChange(Array.from(new Set([...selectedRegionIds, ...filteredRegions.map((region) => region.id)])));
    };

    return (
        <div className={`region-multiselect ${isOpen ? 'is-open' : ''} ${disabled ? 'is-disabled' : ''}`} ref={rootRef}>
            <div className="selector-header-row region-selector-head">
                <strong>Регионы</strong>
                <span>{selectedRegionIds.length} выбрано</span>
            </div>

            <div
                className="region-multiselect-control"
                role="button"
                tabIndex={disabled ? -1 : 0}
                onClick={() => {
                    if (!disabled) {
                        setIsOpen((current) => !current);
                    }
                }}
                onKeyDown={(event) => {
                    if (disabled) {
                        return;
                    }
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        setIsOpen((current) => !current);
                    }
                    if (event.key === 'Escape') {
                        setIsOpen(false);
                    }
                }}
                aria-expanded={isOpen}
                aria-disabled={disabled}
            >
                <div className="region-multiselect-value">
                    {selectedRegions.length === 0 ? (
                        <span className="placeholder-text">Выберите один или несколько регионов</span>
                    ) : (
                        <div className="selected-chip-list region-chip-list">
                            {selectedRegions.slice(0, 15).map((region) => (
                                <span
                                    key={region.id}
                                    className="chip chip-removable"
                                    onClick={(event) => event.stopPropagation()}
                                >
                                    <span>{formatRegionLabel(region)}</span>
                                    <button
                                        type="button"
                                        className="chip-remove-button"
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            toggleRegion(region.id);
                                        }}
                                        aria-label={`Удалить ${region.name}`}
                                    >
                                        ×
                                    </button>
                                </span>
                            ))}
                            {selectedRegions.length > 15 &&
                                <span className="chip">+ ещё {selectedRegions.length - 15}</span>}
                        </div>
                    )}
                </div>
                <span className="region-multiselect-arrow">▾</span>
            </div>

            {isOpen && !disabled && (
                <div className="region-multiselect-menu">
                    <div className="selector-actions region-multiselect-actions">
                        <input
                            ref={searchInputRef}
                            type="search"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Поиск региона"
                        />
                        <div className="inline-actions wrap">
                            <button type="button" onClick={handleSelectVisible} disabled={filteredRegions.length === 0}>
                                Выбрать видимые
                            </button>
                            <button type="button" onClick={() => onChange([])}
                                    disabled={selectedRegionIds.length === 0}>
                                Очистить
                            </button>
                        </div>
                    </div>

                    <div className="region-list region-multiselect-list">
                        {filteredRegions.map((region) => (
                            <label key={region.id} className="check-row region-option-row">
                                <input
                                    type="checkbox"
                                    checked={selectedIdSet.has(region.id)}
                                    onChange={() => toggleRegion(region.id)}
                                />
                                <span>{formatRegionLabel(region)}</span>
                            </label>
                        ))}
                        {filteredRegions.length === 0 && (
                            <div className="empty-state compact">По запросу ничего не найдено.</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
