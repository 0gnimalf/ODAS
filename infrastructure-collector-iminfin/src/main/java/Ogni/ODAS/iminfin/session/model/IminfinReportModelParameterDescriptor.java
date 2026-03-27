package Ogni.ODAS.iminfin.session.model;

public record IminfinReportModelParameterDescriptor(
        String parameterName,
        String valueType,
        boolean required,
        String defaultValue
) {
}
