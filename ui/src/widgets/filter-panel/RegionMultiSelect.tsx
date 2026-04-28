import {useEffect, useMemo, useRef, useState} from 'react';
import {formatRegionLabel} from '../../shared/lib/format';
import type {RegionReadDto} from '../../shared/types/read';

interface RegionMultiSelectProps {
    regions: RegionReadDto[];
    selectedRegionIds: number[];
    onChange: (value: number[]) => void;
    disabled?: boolean;
    mode?: 'single' | 'multiple';
    label?: string;
    helperText?: string;
}

export function RegionMultiSelect({
                                      regions,
                                      selectedRegionIds,
                                      onChange,
                                      disabled = false,
                                      mode = 'multiple',
                                      label = mode === 'single' ? 'Регион' : 'Регионы',
                                      helperText = mode === 'single'
                                          ? 'Выберите один субъект РФ.'
                                          : 'Можно выбрать несколько субъектов РФ.'
                                  }: RegionMultiSelectProps) {
    const rootRef = useRef<HTMLDivElement | null>(null);
    const searchInputRef = useRef<HTMLInputElement | null>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [search, setSearch] = useState('');

    useEffect(() => {
        if (!isOpen) return;

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
        if (!query) return regions;
        return regions.filter((region) => {
            const tokens = [
                region.name,
                region.federalDistrictName,
                region.federalDistrictFullName,
                region.federalDistrictShortName
            ];
            return tokens.some((token) => token.toLowerCase().includes(query));
        });
    }, [regions, search]);

    const labelText = selectedRegions.length === 0
        ? label
        : mode === 'single'
            ? formatRegionLabel(selectedRegions[0])
            : selectedRegions.length <= 2
                ? selectedRegions.map(formatRegionLabel).join(', ')
                : `${selectedRegions.length} регионов выбрано`;

    const toggleRegion = (regionId: number) => {
        if (mode === 'single') {
            onChange(selectedIdSet.has(regionId) ? [] : [regionId]);
            setIsOpen(false);
            return;
        }
        onChange(selectedIdSet.has(regionId)
            ? selectedRegionIds.filter((id) => id !== regionId)
            : [...selectedRegionIds, regionId]);
    };

    const selectAllFiltered = () => {
        const next = new Set(selectedRegionIds);
        filteredRegions.forEach((region) => next.add(region.id));
        onChange(Array.from(next));
    };

    return (
        <div ref={rootRef} className={`region-multiselect ${disabled ? 'is-disabled' : ''}`}>
            <div className="region-selector-head">
                <div>
                    <strong>{label}</strong>
                    <p>{helperText}</p>
                </div>
                <span className="status-badge subtle-badge">{selectedRegionIds.length}</span>
            </div>

            <button
                type="button"
                className="region-multiselect-control"
                onClick={() => !disabled && setIsOpen((current) => !current)}
                disabled={disabled}
            >
                <span className={`region-multiselect-value ${selectedRegions.length === 0 ? 'placeholder-text' : ''}`}>
                    {labelText}
                </span>
                <span className="region-multiselect-arrow">{isOpen ? '⌃' : '⌄'}</span>
            </button>

            {isOpen && !disabled && (
                <div className="region-multiselect-menu">
                    <div className="selector-actions">
                        <input
                            ref={searchInputRef}
                            type="search"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Поиск региона или ФО"
                        />
                        {mode === 'multiple' && (
                            <>
                                <button type="button" className="secondary-button" onClick={selectAllFiltered}>Все
                                </button>
                                <button type="button" className="secondary-button"
                                        onClick={() => onChange([])}>Очистить
                                </button>
                            </>
                        )}
                    </div>

                    <div className="selector-option-list">
                        {filteredRegions.map((region) => {
                            const checked = selectedIdSet.has(region.id);
                            return (
                                <label key={region.id} className={`selector-option ${checked ? 'is-selected' : ''}`}>
                                    <input
                                        type={mode === 'single' ? 'radio' : 'checkbox'}
                                        checked={checked}
                                        onChange={() => toggleRegion(region.id)}
                                    />
                                    <span>{formatRegionLabel(region)}</span>
                                </label>
                            );
                        })}
                        {filteredRegions.length === 0 && (
                            <div className="empty-state compact-empty-state">Регионы не найдены.</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
