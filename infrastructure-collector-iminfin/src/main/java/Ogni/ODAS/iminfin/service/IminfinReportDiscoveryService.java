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

import static Ogni.ODAS.iminfin.util.IminfinUrlNormalizer.*;

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

        String dataUrl = passportDataUrl();
        IminfinReportDefinition definition = discover(page, page.pageUrl(properties), dataUrl);
        cache.put(page, new CachedDefinition(definition, Instant.now()));
        return definition;
    }

    private IminfinReportDefinition discover(IminfinPassportPage page, String pageUrl, String dataUrl) {
        IminfinReportBootstrap bootstrap = loadBootstrap(pageUrl);
        JsonNode metaJson = httpClient.getJson(metaUrl(bootstrap, dataUrl));
        JsonNode primaryJson = httpClient.getJson(primaryUrl(bootstrap, dataUrl));

        return new IminfinReportDefinition(
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
    }

    public String dataUrl(Map<String, ?> queryParameters) {
        return httpClient.withQuery(passportDataUrl(), queryParameters);
    }

    private IminfinReportBootstrap loadBootstrap(String pageUrl) {
        String html = httpClient.getText(pageUrl);
        Matcher matcher = REPORT_LOAD_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to discover report bootstrap on page " + pageUrl);
        }
        return new IminfinReportBootstrap(matcher.group("uuid"), matcher.group("version"));
    }

    private String passportDataUrl() {
        return trimTrailingSlash(properties.getBaseUrl())
                + ensureLeadingSlash(properties.getPassportRoot())
                + "/redirect/copen-imon/Data";
    }

    private String metaUrl(IminfinReportBootstrap bootstrap, String dataUrl) {
        return httpClient.withQuery(
                dataUrl + "/Meta/ReportModel.json",
                Map.of(
                        "reportId", bootstrap.reportId(),
                        "version", bootstrap.version()
                )
        );
    }

    private String primaryUrl(IminfinReportBootstrap bootstrap, String dataUrl) {
        return httpClient.withQuery(
                dataUrl + "/Primary/ReportModel.json",
                Map.of(
                        "reportId", bootstrap.reportId(),
                        "version", bootstrap.version()
                )
        );
    }

    public int loadHelperPeriod(IminfinReportDefinition reportDefinition, String period) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "periodHelperData");
        query.put("paramPeriod", period);

        JsonNode helperResponse = httpClient.getJson(dataUrl(query));
        JsonNode data = helperResponse.path("data");
        if (!data.isArray() || data.isEmpty() || !data.get(0).isArray() || data.get(0).isEmpty()) {
            return 1;
        }
        return data.get(0).get(0).asInt(1);
    }

    public String loadLatestPeriod(IminfinReportDefinition reportDefinition) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "ds_FK_Passport_MONTH_Periods");
        query.put("verified", true);
        query.put("latest", false);

        JsonNode response = httpClient.getJson(dataUrl(query));
        JsonNode data = response.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new IllegalStateException("No periods available for report " + reportDefinition.title());
        }

        JsonNode last = data.get(data.size() - 1);
        if (!last.isArray() || last.isEmpty()) {
            throw new IllegalStateException("Unexpected period payload for report " + reportDefinition.title());
        }
        return last.get(0).asText();
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

    private record CachedDefinition(IminfinReportDefinition definition, Instant discoveredAt) {
        boolean isExpired(IminfinCollectorProperties properties) {
            return discoveredAt.plus(properties.getDiscoveryTtl()).isBefore(Instant.now());
        }
    }
}
