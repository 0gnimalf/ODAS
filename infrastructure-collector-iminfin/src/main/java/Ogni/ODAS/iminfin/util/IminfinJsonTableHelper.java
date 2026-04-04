package Ogni.ODAS.iminfin.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IminfinJsonTableHelper {

    private IminfinJsonTableHelper() {
    }

    public static Map<String, Integer> columnIndexes(List<String> columnNames) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (columnNames == null) {
            return result;
        }

        for (int i = 0; i < columnNames.size(); i++) {
            result.put(columnNames.get(i), i);
        }
        return result;
    }

    public static String textCell(JsonNode row, Integer index) {
        if (!hasCell(row, index)) {
            return null;
        }
        JsonNode node = row.get(index);
        return node == null || node.isNull() ? null : node.asText();
    }

    public static BigDecimal decimalCell(JsonNode row, Integer index) {
        if (!hasCell(row, index)) {
            return null;
        }

        JsonNode node = row.get(index);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }

        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(text.replace(" ", "").replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Long longCell(JsonNode row, Integer index) {
        BigDecimal value = decimalCell(row, index);
        return value == null ? null : value.longValue();
    }

    private static boolean hasCell(JsonNode row, Integer index) {
        return row != null && row.isArray() && index != null && index >= 0 && index < row.size();
    }
}
