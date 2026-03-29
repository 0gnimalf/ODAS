package Ogni.ODAS.iminfin.model;

public record IminfinParameterDefinition(
        String name,
        String type,
        String defaultValue,
        boolean periodParameter
) {
}
