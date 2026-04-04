package Ogni.ODAS.iminfin.model;

import com.fasterxml.jackson.databind.JsonNode;

public record IminfinLoadedData(
        String dataSourceCode,
        IminfinDataSourceDefinition dataSource,
        JsonNode dataRows
) {
}
