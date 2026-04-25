import type {IndicatorTreeNodeReadDto} from '../types/read';

export function countTreeNodes(nodes: IndicatorTreeNodeReadDto[]): number {
    return nodes.reduce((total, node) => total + 1 + countTreeNodes(node.children), 0);
}

export function collectNodeNamesByIds(
    nodes: IndicatorTreeNodeReadDto[],
    selectedIds: Set<number>
): string[] {
    const names: string[] = [];

    const visit = (items: IndicatorTreeNodeReadDto[]) => {
        items.forEach((item) => {
            if (selectedIds.has(item.id)) {
                names.push(item.name);
            }
            if (item.children.length > 0) {
                visit(item.children);
            }
        });
    };

    visit(nodes);
    return names;
}

export function filterTreeByQuery(
    nodes: IndicatorTreeNodeReadDto[],
    query: string
): IndicatorTreeNodeReadDto[] {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) {
        return nodes;
    }

    return nodes
        .map((node) => {
            const children = filterTreeByQuery(node.children, normalizedQuery);
            const selfMatches = node.name.toLowerCase().includes(normalizedQuery);

            if (!selfMatches && children.length === 0) {
                return null;
            }

            return {
                ...node,
                children
            };
        })
        .filter((node): node is IndicatorTreeNodeReadDto => node !== null);
}

export function collectAllNodeIds(nodes: IndicatorTreeNodeReadDto[]): number[] {
    const result: number[] = [];

    const visit = (items: IndicatorTreeNodeReadDto[]) => {
        items.forEach((item) => {
            result.push(item.id);
            if (item.children.length > 0) {
                visit(item.children);
            }
        });
    };

    visit(nodes);
    return result;
}

export function expandSelectedIdsWithDescendants(
    nodes: IndicatorTreeNodeReadDto[],
    selectedIds: number[]
): number[] {
    if (selectedIds.length === 0) {
        return [];
    }

    const selectedIdSet = new Set(selectedIds);
    const expandedIds = new Set<number>();

    const collectDescendants = (items: IndicatorTreeNodeReadDto[]) => {
        items.forEach((item) => {
            expandedIds.add(item.id);
            if (item.children.length > 0) {
                collectDescendants(item.children);
            }
        });
    };

    const visit = (items: IndicatorTreeNodeReadDto[]) => {
        items.forEach((item) => {
            if (selectedIdSet.has(item.id)) {
                expandedIds.add(item.id);
                collectDescendants(item.children);
            } else if (item.children.length > 0) {
                visit(item.children);
            }
        });
    };

    visit(nodes);
    return Array.from(expandedIds);
}
