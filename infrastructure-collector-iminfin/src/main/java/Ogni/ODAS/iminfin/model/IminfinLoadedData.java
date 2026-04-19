package Ogni.ODAS.iminfin.model;

import com.fasterxml.jackson.databind.JsonNode;

public record IminfinLoadedData(
        String request,
        String rawData,
        String dataSourceCode,
        IminfinDataSourceDefinition dataSource,
        JsonNode dataRows
) {
}
