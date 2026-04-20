package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinParsedIndicatorRow;
import Ogni.ODAS.iminfin.util.IminfinJsonTableHelper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class IminfinIndicatorTreeParser {

    public List<IminfinParsedIndicatorRow> parseDetailRows(
            String namespace,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows
    ) {
        if (!dataRows.isArray()) {
            throw new IllegalStateException("Unexpected iMinfin dataset payload: data must be an array");
        }
        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(dataSource.columnNames());
        Integer nameIndex = columns.get("name");
        if (nameIndex == null) {
            throw new IllegalStateException("Detail dataset does not contain 'name' column");
        }
        Integer levelIndex = columns.get("level");
        Map<Integer, String> lastKeyByLevel = new HashMap<>();
        Map<Integer, String> lastNameByLevel = new HashMap<>();
        Set<String> usedKeys = new HashSet<>();
        List<IntermediateRow> intermediate = new ArrayList<>();

        int sortOrder = 0;
        for (JsonNode row : dataRows) {
            String caption = IminfinJsonTableHelper.textCell(row, nameIndex);
            if (caption == null || caption.isBlank()) {
                continue;
            }
            sortOrder++;
            int externalLevel = intCell(row, levelIndex, 1);
            int level = Math.max(0, externalLevel - 1);
            lastKeyByLevel.keySet().removeIf(existing -> existing >= level + 1);
            lastNameByLevel.keySet().removeIf(existing -> existing >= level + 1);
            String parentKey = level == 0 ? null : lastKeyByLevel.get(level - 1);
            String parentName = level == 0 ? null : lastNameByLevel.get(level - 1);
            String name = caption.trim();
            String key = buildNaturalKey(namespace, parentKey, name, sortOrder, usedKeys);
            lastKeyByLevel.put(level, key);
            lastNameByLevel.put(level, name);
            intermediate.add(new IntermediateRow(key, parentKey, name, parentName, level, sortOrder, row));
        }

        Set<String> parentKeys = new HashSet<>();
        intermediate.stream()
                .map(IntermediateRow::parentNaturalKey)
                .filter(Objects::nonNull)
                .forEach(parentKeys::add);

        return intermediate.stream()
                .map(row -> new IminfinParsedIndicatorRow(
                        row.naturalKey(),
                        row.parentNaturalKey(),
                        row.name(),
                        row.parentName(),
                        row.level(),
                        row.sortOrder(),
                        parentKeys.contains(row.naturalKey()),
                        row.row()
                ))
                .toList();
    }

    private String buildNaturalKey(String namespace, String parentKey, String caption, int sortOrder, Set<String> usedKeys) {
        String segment = TextNormalizer.slugify(caption);
        if (segment.isBlank()) {
            segment = "indicator-" + sortOrder;
        }
        String base = parentKey != null && !parentKey.isBlank()
                ? parentKey + "/" + segment
                : namespace + "/" + segment;
        String key = base;
        int duplicateIndex = 2;
        while (!usedKeys.add(key)) {
            key = base + "-" + duplicateIndex;
            duplicateIndex++;
        }
        return key;
    }

    private int intCell(JsonNode row, Integer index, int defaultValue) {
        String text = IminfinJsonTableHelper.textCell(row, index);
        if (text == null || text.isBlank()) {
            JsonNode node = index == null || index < 0 || index >= row.size() ? null : row.get(index);
            return node != null && node.isInt() ? node.asInt() : defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private record IntermediateRow(
            String naturalKey,
            String parentNaturalKey,
            String name,
            String parentName,
            int level,
            int sortOrder,
            JsonNode row
    ) {
    }
}
