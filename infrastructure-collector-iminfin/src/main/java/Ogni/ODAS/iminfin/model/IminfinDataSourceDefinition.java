package Ogni.ODAS.iminfin.model;

import java.util.List;

public record IminfinDataSourceDefinition(
        String name,
        List<String> parameters,
        List<String> columnNames,
        boolean fixed,
        List<List<String>> fixedData,
        String externalSourceCode
) {
}
