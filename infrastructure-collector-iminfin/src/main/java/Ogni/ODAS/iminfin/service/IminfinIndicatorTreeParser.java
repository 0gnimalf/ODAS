package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class IminfinIndicatorTreeParser {

    public List<IminfinParsedIndicatorRow> parseDetailRows(
            String rootPrefix,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows
    ) {
        if (!dataRows.isArray()) {
            throw new IllegalStateException("Unexpected iMinfin dataset payload: data must be an array");
        }

        Map<String, Integer> columns = columnIndexes(dataSource.columnNames());
        Integer nameIndex = columns.get("name");
        if (nameIndex == null) {
            throw new IllegalStateException("Detail dataset does not contain 'name' column");
        }

        Integer levelIndex = columns.get("level");
        Map<Integer, String> lastCodeByLevel = new HashMap<>();
        Set<String> usedCodes = new HashSet<>();
        List<IminfinParsedIndicatorRow> result = new ArrayList<>();

        int sortOrder = 0;
        for (JsonNode row : dataRows) {
            if (!row.isArray() || row.isEmpty()) {
                continue;
            }

            String caption = textCell(row, nameIndex);
            if (caption == null || caption.isBlank()) {
                continue;
            }

            sortOrder++;
            int level = levelIndex == null ? 1 : intCell(row, levelIndex, 1);
            if (level < 1) {
                level = 1;
            }

            int finalLevel = level;
            lastCodeByLevel.keySet().removeIf(existingLevel -> existingLevel >= finalLevel + 1);
            String parentCode = level == 1 ? null : lastCodeByLevel.get(level - 1);
            String code = buildCode(rootPrefix, parentCode, caption, sortOrder, usedCodes);
            lastCodeByLevel.put(level, code);

            result.add(new IminfinParsedIndicatorRow(
                    code,
                    parentCode,
                    caption.trim(),
                    level,
                    sortOrder,
                    isSection(row, columns),
                    row
            ));
        }

        return result;
    }

    public String buildCode(String rootPrefix, String parentCode, String caption, int sortOrder, Set<String> usedCodes) {
        String segment = IminfinTextNormalizer.slugify(caption);
        if (segment.isBlank()) {
            segment = "indicator-" + sortOrder;
        }

        String base;
        if (parentCode != null && !parentCode.isBlank()) {
            base = parentCode + "/" + segment;
        } else if (rootPrefix != null && !rootPrefix.isBlank()) {
            base = rootPrefix + "/" + segment;
        } else {
            base = segment;
        }

        String code = base;
        int duplicateIndex = 2;
        while (!usedCodes.add(code)) {
            code = base + "-" + duplicateIndex;
            duplicateIndex++;
        }
        return code;
    }

    private Map<String, Integer> columnIndexes(List<String> columnNames) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            result.put(columnNames.get(i), i);
        }
        return result;
    }

    private boolean isSection(JsonNode row, Map<String, Integer> columns) {
        for (Map.Entry<String, Integer> entry : columns.entrySet()) {
            String name = entry.getKey();
            if ("name".equals(name) || "level".equals(name)) {
                continue;
            }
            if (entry.getValue() >= row.size()) {
                continue;
            }
            JsonNode node = row.get(entry.getValue());
            if (node != null && !node.isNull() && !node.asText().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String textCell(JsonNode row, int index) {
        if (index >= row.size()) {
            return null;
        }
        JsonNode node = row.get(index);
        return node == null || node.isNull() ? null : node.asText();
    }

    private int intCell(JsonNode row, int index, int defaultValue) {
        if (index >= row.size()) {
            return defaultValue;
        }
        JsonNode node = row.get(index);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        if (node.isInt()) {
            return node.asInt();
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(text);
    }
}
