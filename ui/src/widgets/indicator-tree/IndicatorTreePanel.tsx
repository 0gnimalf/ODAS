import {useMemo, useState} from 'react';
import {collectAllNodeIds, collectNodeNamesByIds, countTreeNodes, filterTreeByQuery} from '../../shared/lib/tree';
import type {IndicatorTreeNodeReadDto} from '../../shared/types/read';

interface IndicatorTreePanelProps {
    tree: IndicatorTreeNodeReadDto[];
    loading: boolean;
    error: string | null;
    selectedIds: number[];
    includeChildren: boolean;
    onSelectedIdsChange: (value: number[]) => void;
    onIncludeChildrenChange: (value: boolean) => void;
}

export function IndicatorTreePanel({
                                       tree,
                                       loading,
                                       error,
                                       selectedIds,
                                       includeChildren,
                                       onSelectedIdsChange,
                                       onIncludeChildrenChange
                                   }: IndicatorTreePanelProps) {
    const [search, setSearch] = useState('');
    const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

    const selectedIdSet = useMemo(() => new Set(selectedIds), [selectedIds]);
    const filteredTree = useMemo(() => filterTreeByQuery(tree, search), [tree, search]);
    const selectedNames = useMemo(() => collectNodeNamesByIds(tree, selectedIdSet), [tree, selectedIdSet]);
    const visibleNodeIds = useMemo(() => collectAllNodeIds(filteredTree), [filteredTree]);
    const totalNodes = useMemo(() => countTreeNodes(tree), [tree]);

    const toggleSelected = (id: number) => {
        const next = new Set(selectedIds);
        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }
        onSelectedIdsChange(Array.from(next));
    };

    const toggleExpanded = (id: number) => {
        const next = new Set(expandedIds);
        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }
        setExpandedIds(next);
    };

    const isSearchMode = search.trim().length > 0;

    return (
        <section className="panel tree-panel">
            <div className="panel-header compact-gap">
                <div>
                    <h2>Дерево показателей</h2>
                    <p>{totalNodes > 0 ? `Загружено узлов: ${totalNodes}` : 'Выберите группу и год, чтобы загрузить дерево.'}</p>
                </div>
            </div>

            <div className="selector-actions tree-actions">
                <input
                    type="search"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Поиск по дереву"
                    disabled={tree.length === 0 || loading}
                />
                <div className="inline-actions wrap">
                    <button
                        type="button"
                        onClick={() => onSelectedIdsChange(Array.from(new Set([...selectedIds, ...visibleNodeIds])))}
                        disabled={visibleNodeIds.length === 0}
                    >
                        Выбрать видимые
                    </button>
                    <button type="button" onClick={() => onSelectedIdsChange([])} disabled={selectedIds.length === 0}>
                        Очистить
                    </button>
                </div>
            </div>

            <label className="check-row include-children-row">
                <input type="checkbox" checked={includeChildren}
                       onChange={(event) => onIncludeChildrenChange(event.target.checked)}/>
                <span>Запросить также дочерние показатели</span>
            </label>

            <div className="selected-summary">
                <strong>Выбрано узлов: {selectedIds.length}</strong>
                {selectedNames.length > 0 && (
                    <div className="selected-chip-list">
                        {selectedNames.slice(0, 8).map((name) => (
                            <span key={name} className="chip">
                {name}
              </span>
                        ))}
                        {selectedNames.length > 8 && <span className="chip">+ ещё {selectedNames.length - 8}</span>}
                    </div>
                )}
            </div>

            <div className="tree-content">
                {loading && <div className="empty-state">Загрузка дерева…</div>}
                {!loading && error && <div className="error-state">{error}</div>}
                {!loading && !error && filteredTree.length === 0 &&
                    <div className="empty-state">Нет узлов для отображения.</div>}
                {!loading && !error && filteredTree.length > 0 && (
                    <div className="tree-scroll">
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
                <button
                    type="button"
                    className="tree-toggle"
                    onClick={() => hasChildren && onToggleExpanded(node.id)}
                    aria-label={isExpanded ? 'Свернуть' : 'Развернуть'}
                >
                    {hasChildren ? (isExpanded ? '▾' : '▸') : '·'}
                </button>
                <label className="check-row tree-check-row">
                    <input type="checkbox" checked={selectedIdSet.has(node.id)}
                           onChange={() => onToggleSelected(node.id)}/>
                    <span>{node.name}</span>
                </label>
            </div>

            {hasChildren && isExpanded && (
                <div>
                    {node.children.map((child) => (
                        <TreeNodeRow
                            key={child.id}
                            node={child}
                            level={level + 1}
                            selectedIdSet={selectedIdSet}
                            expandedIds={expandedIds}
                            isSearchMode={isSearchMode}
                            onToggleExpanded={onToggleExpanded}
                            onToggleSelected={onToggleSelected}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
