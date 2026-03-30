package Ogni.ODAS.iminfin.service;

import com.fasterxml.jackson.databind.JsonNode;

public record IminfinParsedIndicatorRow(
        String code,
        String parentCode,
        String caption,
        int level,
        int sortOrder,
        boolean section,
        JsonNode row
) {
}
