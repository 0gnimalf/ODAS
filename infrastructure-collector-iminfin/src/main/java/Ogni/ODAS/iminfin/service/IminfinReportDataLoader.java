package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.support.JsonSupport;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinLoadedData;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class IminfinReportDataLoader {

    private static final String PERIOD_PARAMETER = "paramPeriod";
    private static final String TERRITORY_PARAMETER = "territory";
    private static final String OUTCOMES_TYPE_PARAMETER = "PassportFK_002_002_outcomesType";

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinHttpClient httpClient;

    public IminfinReportDataLoader(
            IminfinReportDiscoveryService discoveryService,
            IminfinHttpClient httpClient
    ) {
        this.discoveryService = Objects.requireNonNull(discoveryService);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    public IminfinLoadedData loadDetailData(
            IminfinReportDefinition reportDefinition,
            String territoryCode,
            String period,
            Integer outcomesType
    ) {
        int helperPeriod = discoveryService.loadHelperPeriod(reportDefinition, period);
        String dataSourceCode = reportDefinition.resolveDetailDataSource(helperPeriod);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(TERRITORY_PARAMETER, territoryCode);
        parameters.put(PERIOD_PARAMETER, period);
        if (outcomesType != null) {
            parameters.put(OUTCOMES_TYPE_PARAMETER, outcomesType);
        }
        return loadData(reportDefinition, dataSourceCode, parameters);
    }

    public IminfinLoadedData loadData(
            IminfinReportDefinition reportDefinition,
            String dataSourceCode,
            Map<String, ?> extraParameters
    ) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", dataSourceCode);
        if (extraParameters != null && !extraParameters.isEmpty()) {
            query.putAll(extraParameters);
        }
        String request = discoveryService.dataUrl(query);
        JsonNode response = httpClient.getJson(request);
        return new IminfinLoadedData(
                request,
                JsonSupport.write(response),
                dataSourceCode,
                reportDefinition.requireDataSource(dataSourceCode),
                response.path("data")
        );
    }
}
