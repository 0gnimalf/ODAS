package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinParameterDefinition;
import Ogni.ODAS.iminfin.model.IminfinReportBootstrap;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IminfinReportDiscoveryService {

    private static final Pattern REPORT_LOAD_PATTERN = Pattern.compile(
            "WebReports\\.ReportContainer\\.load\\(\\s*\\{\\s*uuid:\\s*\"(?<uuid>[^\"]+)\",\\s*version:\\s*\"(?<version>[^\"]+)\"",
            Pattern.DOTALL
    );
    private final IminfinHttpClient httpClient;
    private final IminfinCollectorProperties properties;
    private final Map<IminfinPassportPage, CachedDefinition> cache = new HashMap<>();

    public IminfinReportDiscoveryService(IminfinHttpClient httpClient, IminfinCollectorProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public synchronized IminfinReportDefinition discover(IminfinPassportPage page) {
        CachedDefinition cachedDefinition = cache.get(page);
        if (cachedDefinition != null && !cachedDefinition.isExpired(properties)) {
            return cachedDefinition.definition();
        }

        IminfinReportBootstrap bootstrap = loadBootstrap(page);
        JsonNode metaJson = httpClient.getJson(metaUrl(bootstrap));
        JsonNode primaryJson = httpClient.getJson(primaryUrl(bootstrap));

        IminfinReportDefinition definition = new IminfinReportDefinition(
                page,
                bootstrap.reportId(),
                metaJson.path("uuid").asText(bootstrap.reportId()),
                metaJson.path("version").asText(bootstrap.version()),
                metaJson.path("dataVersion").asText(metaJson.path("version").asText(bootstrap.version())),
                metaJson.path("title").asText(page.name()),
                parseParameters(metaJson.path("dataParameters")),
                parseDataSources(metaJson.path("dataSources")),
                parseViewMainDataSources(metaJson.path("reportViews")),
                parsePrimarySources(primaryJson)
        );

        cache.put(page, new CachedDefinition(definition, Instant.now()));
        return definition;
    }

    public String dataUrl(Map<String, ?> queryParameters) {
        return httpClient.withQuery(baseDataUrl(), queryParameters);
    }

    private IminfinReportBootstrap loadBootstrap(IminfinPassportPage page) {
        String html = httpClient.getText(page.pageUrl(properties));
        Matcher matcher = REPORT_LOAD_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to discover report bootstrap on page " + page.pageUrl(properties));
        }
        return new IminfinReportBootstrap(matcher.group("uuid"), matcher.group("version"));
    }

    private String baseDataUrl() {
        return trimTrailingSlash(properties.getBaseUrl())
                + ensureLeadingSlash(properties.getPassportRoot())
                + "/redirect/copen-imon/Data";
    }

    private String metaUrl(IminfinReportBootstrap bootstrap) {
        return httpClient.withQuery(
                baseDataUrl() + "/Meta/ReportModel.json",
                Map.of(
                        "reportId", bootstrap.reportId(),
                        "version", bootstrap.version()
                )
        );
    }

    private String primaryUrl(IminfinReportBootstrap bootstrap) {
        return httpClient.withQuery(
                baseDataUrl() + "/Primary/ReportModel.json",
                Map.of(
                        "reportId", bootstrap.reportId(),
                        "version", bootstrap.version()
                )
        );
    }

    private Map<String, IminfinParameterDefinition> parseParameters(JsonNode parametersNode) {
        Map<String, IminfinParameterDefinition> result = new LinkedHashMap<>();
        if (!parametersNode.isArray()) {
            return result;
        }

        for (JsonNode parameterNode : parametersNode) {
            String name = parameterNode.path("name").asText();
            result.put(name, new IminfinParameterDefinition(
                    name,
                    parameterNode.path("type").asText(),
                    parameterNode.path("defaultValue").isMissingNode() || parameterNode.path("defaultValue").isNull()
                            ? null
                            : parameterNode.path("defaultValue").asText(),
                    parameterNode.path("isParamPeriod").asBoolean(false)
            ));
        }
        return result;
    }

    private Map<String, IminfinDataSourceDefinition> parseDataSources(JsonNode dataSourcesNode) {
        Map<String, IminfinDataSourceDefinition> result = new LinkedHashMap<>();
        if (!dataSourcesNode.isArray()) {
            return result;
        }

        for (JsonNode dataSourceNode : dataSourcesNode) {
            String name = dataSourceNode.path("name").asText();
            List<String> parameters = new ArrayList<>();
            dataSourceNode.path("parameters").forEach(node -> parameters.add(node.asText()));

            List<String> columns = new ArrayList<>();
            dataSourceNode.path("columnsMetaData").path("types").forEach(node -> columns.add(node.path("name").asText()));

            List<List<String>> fixedData = new ArrayList<>();
            dataSourceNode.path("fixedData").forEach(row -> {
                List<String> values = new ArrayList<>();
                row.forEach(cell -> values.add(cell.isNull() ? null : cell.asText()));
                fixedData.add(values);
            });

            result.put(name, new IminfinDataSourceDefinition(
                    name,
                    List.copyOf(parameters),
                    List.copyOf(columns),
                    dataSourceNode.path("control").asText().contains("FixedDataSource"),
                    List.copyOf(fixedData),
                    dataSourceNode.path("externalSourceRef").path("code").asText(null)
            ));
        }

        return result;
    }

    private Map<String, String> parseViewMainDataSources(JsonNode reportViewsNode) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!reportViewsNode.isArray()) {
            return result;
        }

        for (JsonNode viewNode : reportViewsNode) {
            JsonNode reportNode = viewNode.path("report");
            String viewName = reportNode.path("name").asText();
            String mainDataSource = null;
            for (JsonNode dataSourceRef : reportNode.path("dataSourcesRefs")) {
                if (dataSourceRef.path("main").asBoolean(false)) {
                    mainDataSource = dataSourceRef.path("code").asText(null);
                    break;
                }
            }
            result.put(viewName, mainDataSource);
        }
        return result;
    }

    private List<String> parsePrimarySources(JsonNode primaryNode) {
        List<String> result = new ArrayList<>();
        primaryNode.path("externalSourcesInfo").forEach(sourceInfo -> result.add(
                sourceInfo.path("externalSourceRef").path("code").asText()
        ));
        return List.copyOf(result);
    }

    private static String ensureLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record CachedDefinition(IminfinReportDefinition definition, Instant loadedAt) {
        boolean isExpired(IminfinCollectorProperties properties) {
            return loadedAt.plus(properties.getDiscoveryTtl()).isBefore(Instant.now());
        }
    }
}
