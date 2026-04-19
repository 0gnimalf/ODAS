package Ogni.ODAS.iminfin.model;

import Ogni.ODAS.iminfin.config.IminfinPassportPage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record IminfinReportDefinition(
        IminfinPassportPage page,
        String reportId,
        String uuid,
        String version,
        String dataVersion,
        OffsetDateTime dataVersionDate,
        String title,
        Map<String, IminfinParameterDefinition> parameters,
        Map<String, IminfinDataSourceDefinition> dataSources,
        Map<String, String> viewMainDataSources,
        List<String> externalSourceCodes
) {
    public IminfinReportDefinition {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        dataSources = dataSources == null ? Map.of() : Map.copyOf(dataSources);
        viewMainDataSources = viewMainDataSources == null ? Map.of() : Map.copyOf(viewMainDataSources);
        externalSourceCodes = externalSourceCodes == null ? List.of() : List.copyOf(externalSourceCodes);
    }

    public IminfinDataSourceDefinition requireDataSource(String name) {
        IminfinDataSourceDefinition definition = dataSources.get(name);
        if (definition == null) {
            throw new IllegalStateException("Data source not found in report model: " + name + " (report=" + title + ")");
        }
        return definition;
    }

    public String defaultValue(String parameterName) {
        IminfinParameterDefinition parameterDefinition = parameters.get(parameterName);
        return parameterDefinition == null ? null : parameterDefinition.defaultValue();
    }

    public String resolveDetailDataSource(int helperPeriod) {
        if (helperPeriod >= 2) {
            String after = viewMainDataSources.entrySet().stream()
                    .filter(entry -> entry.getKey().contains("After"))
                    .map(Map.Entry::getValue)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (after != null) {
                return after;
            }
        }

        return viewMainDataSources.entrySet().stream()
                .filter(entry -> entry.getKey().contains("Before") || viewMainDataSources.size() == 1)
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to resolve detail data source for report " + title));
    }
}
