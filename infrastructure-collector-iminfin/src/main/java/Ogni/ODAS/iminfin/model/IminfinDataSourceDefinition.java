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
    public IminfinDataSourceDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        columnNames = columnNames == null ? List.of() : List.copyOf(columnNames);
        fixedData = fixedData == null ? List.of() : List.copyOf(fixedData);
    }
}
