package Ogni.ODAS.iminfin.model;

import com.fasterxml.jackson.databind.JsonNode;

public record IminfinParsedIndicatorRow(
        String naturalKey,
        String parentNaturalKey,
        String name,
        String parentName,
        int level,
        int sortOrder,
        boolean hasChildren,
        JsonNode row
) {
}
