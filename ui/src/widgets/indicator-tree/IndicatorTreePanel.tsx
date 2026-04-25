import {useEffect, useMemo, useState} from 'react';
import {collectAllNodeIds, collectNodeNamesByIds, countTreeNodes, filterTreeByQuery} from '../../shared/lib/tree';
import type {IndicatorTreeNodeReadDto} from '../../shared/types/read';

interface IndicatorTreePanelProps {
    tree: IndicatorTreeNodeReadDto[];
    loading: boolean;
    error: string | null;
    selectedIds: number[];
    includeChildren: boolean;
    onSelectedIdsChange: (next: number[]) => void;
    onIncludeChildrenChange: (next: boolean) => void;
    selectionMode?: 'single' | 'multiple';
    embedded?: boolean;
    showIncludeChildrenOption?: boolean;
    onSyncTree?: () => void;
    canSyncTree?: boolean;
    syncingTree?: boolean;
}

export function IndicatorTreePanel({
                                       tree,
                                       loading,
                                       error,
                                       selectedIds,
                                       includeChildren,
                                       onSelectedIdsChange,
                                       onIncludeChildrenChange,
                                       selectionMode = 'multiple',
                                       embedded = false,
                                       showIncludeChildrenOption = true,
                                       onSyncTree,
                                       canSyncTree = false,
                                       syncingTree = false
                                   }: IndicatorTreePanelProps) {
    const [search, setSearch] = useState('');
    const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

    const isSearchMode = search.trim().length > 0;
    const filteredTree = useMemo(() => filterTreeByQuery(tree, search), [tree, search]);
    const selectedIdSet = useMemo(() => new Set(selectedIds), [selectedIds]);
    const selectedNames = useMemo(() => collectNodeNamesByIds(tree, selectedIdSet), [tree, selectedIdSet]);
    const totalNodeCount = useMemo(() => countTreeNodes(tree), [tree]);

    useEffect(() => {
        if (isSearchMode) {
            setExpandedIds(new Set(collectAllNodeIds(filteredTree)));
        }
    }, [filteredTree, isSearchMode]);

    const toggleExpanded = (id: number) => {
        setExpandedIds((current) => {
            const next = new Set(current);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const toggleSelected = (id: number) => {
        if (selectionMode === 'single') {
            onSelectedIdsChange(selectedIdSet.has(id) ? [] : [id]);
            return;
        }

        const next = new Set(selectedIds);
        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }
        onSelectedIdsChange(Array.from(next));
    };

    const containerClassName = embedded ? 'tree-panel-embedded' : 'panel';

    return (
        <section className={containerClassName}>
            <div className="selector-header-row tree-selector-head">
                <div>
                    <h3>Дерево показателей</h3>
                    <p>
                        {tree.length > 0
                            ? `Загружено ${totalNodeCount} узл. Выбрано: ${selectedIds.length}.`
                            : 'Дерево ещё не загружено.'}
                    </p>
                </div>
                <span className="chip">
          {selectionMode === 'single' ? 'Один показатель' : 'Несколько показателей'}
        </span>
            </div>

            <div className="selector-actions tree-actions">
                <label className="field compact-search-field">
                    <span>Поиск по названию</span>
                    <input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Например, Налоговые доходы"
                    />
                </label>
            </div>

            <div className="tree-selection-summary">
                {selectedNames.length > 0 ? (
                    <div className="selected-chip-list">
                        {selectedNames.map((name) => (
                            <span key={name} className="chip" title={name}>
                {name}
              </span>
                        ))}
                    </div>
                ) : (
                    <span className="placeholder-text">Показатели ещё не выбраны.</span>
                )}
            </div>

            {showIncludeChildrenOption && (
                <div className="checkbox-grid tree-option-grid">
                    <label className="check-row checkbox-card compact-checkbox-card">
                        <input
                            type="checkbox"
                            checked={includeChildren}
                            onChange={(event) => onIncludeChildrenChange(event.target.checked)}
                        />
                        <span>Учитывать поддерево выбранного узла</span>
                    </label>
                </div>
            )}

            <div className="tree-content tree-content-embedded">
                {loading && <div className="empty-state">Загрузка дерева…</div>}
                {!loading && error && <div className="error-state">{error}</div>}
                {!loading && !error && filteredTree.length === 0 && tree.length > 0 &&
                    <div className="empty-state">Нет узлов для отображения.</div>}
                {!loading && !error && tree.length === 0 && (
                    <div className="tree-empty-state">
                        <div className="empty-state compact">Для выбранных группы и года дерево пока не загружено.</div>
                        {onSyncTree && (
                            <button type="button" className="primary-button" onClick={onSyncTree}
                                    disabled={!canSyncTree || syncingTree}>
                                {syncingTree ? 'Синхронизация…' : 'Синхронизировать дерево'}
                            </button>
                        )}
                    </div>
                )}
                {!loading && !error && filteredTree.length > 0 && (
                    <div className="tree-scroll tree-scroll-embedded">
                        {filteredTree.map((node) => (
                            <TreeNodeRow
                                key={node.id}
                                node={node}
                                level={0}
                                selectedIdSet={selectedIdSet}
                                expandedIds={expandedIds}
                                isSearchMode={isSearchMode}
                                onToggleExpanded={toggleExpanded}
                                onToggleSelected={toggleSelected}
                            />
                        ))}
                    </div>
                )}
            </div>
        </section>
    );
}

interface TreeNodeRowProps {
    node: IndicatorTreeNodeReadDto;
    level: number;
    selectedIdSet: Set<number>;
    expandedIds: Set<number>;
    isSearchMode: boolean;
    onToggleExpanded: (id: number) => void;
    onToggleSelected: (id: number) => void;
}

function TreeNodeRow({
                         node,
                         level,
                         selectedIdSet,
                         expandedIds,
                         isSearchMode,
                         onToggleExpanded,
                         onToggleSelected
                     }: TreeNodeRowProps) {
    const hasChildren = node.children.length > 0;
    const isExpanded = isSearchMode || expandedIds.has(node.id);
    return (
        <div className="tree-node">
            <div className="tree-node-row" style={{paddingLeft: `${level * 18}px`}}>
                <button type="button" className="tree-toggle" onClick={() => hasChildren && onToggleExpanded(node.id)}
                        aria-label={isExpanded ? 'Свернуть' : 'Развернуть'}>
                    {hasChildren ? (isExpanded ? '▾' : '▸') : '·'}
                </button>
                <label className="check-row tree-check-row">
                    <input type="checkbox" checked={selectedIdSet.has(node.id)}
                           onChange={() => onToggleSelected(node.id)}/>
                    <span title={node.name}>{node.name}</span>
                </label>
            </div>
            {hasChildren && isExpanded && node.children.map((child) => (
                <TreeNodeRow key={child.id} node={child} level={level + 1} selectedIdSet={selectedIdSet}
                             expandedIds={expandedIds} isSearchMode={isSearchMode} onToggleExpanded={onToggleExpanded}
                             onToggleSelected={onToggleSelected}/>
            ))}
        </div>
    );
}
