package Ogni.ODAS.application.iminfin.model;

public record IminfinParameterCatalogItem(
        Long id,
        IminfinReportProfileKey profileKey,
        String parameterName,
        IminfinParameterRole role,
        String valueType,
        boolean required,
        String defaultValue
) {
}
