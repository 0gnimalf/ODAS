package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IminfinReportDiscoveryServiceTest {

    @Mock
    private IminfinHttpClient httpClient;

    private IminfinCollectorProperties properties;
    private IminfinReportDiscoveryService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new IminfinCollectorProperties();
        properties.setDiscoveryTtl(Duration.ofHours(1));
        service = new IminfinReportDiscoveryService(httpClient, properties);
    }

    @Test
    void discoverParsesBootstrapAndCachesDefinition() throws Exception {
        when(httpClient.getText(contains("dokhody-detalno"))).thenReturn(
                "WebReports.ReportContainer.load({ uuid: \"report-1\", version: \"version-1\" })"
        );
        when(httpClient.withQuery(contains("/Meta/ReportModel.json"), anyMap())).thenCallRealMethod();
        when(httpClient.withQuery(contains("/Primary/ReportModel.json"), anyMap())).thenCallRealMethod();
        when(httpClient.getJson(contains("/Meta/ReportModel.json"))).thenReturn(objectMapper.readTree("""
                {
                  "uuid": "uuid-1",
                  "version": "version-1",
                  "dataVersion": "dv-1",
                  "title": "Доходы",
                  "dataParameters": [
                    {"name":"territory","type":"string","defaultValue":"45000000"}
                  ],
                  "dataSources": [
                    {"name":"PassportFK_002_001_incomesDataAfter01052019","parameters":["territory"],"columnsMetaData":{"types":[{"name":"name"}]}}
                  ],
                  "reportViews": [
                    {"report":{"name":"ViewAfter","dataSourcesRefs":[{"code":"PassportFK_002_001_incomesDataAfter01052019","main":true}]}}
                  ]
                }
                """));
        when(httpClient.getJson(contains("/Primary/ReportModel.json"))).thenReturn(objectMapper.readTree("""
                {"externalSourcesInfo":[{"externalSourceRef":{"code":"FK_Passport_MONTH"}}]}
                """));

        var first = service.discover(IminfinPassportPage.INCOMES_DETAIL);
        var second = service.discover(IminfinPassportPage.INCOMES_DETAIL);

        assertEquals("uuid-1", first.uuid());
        assertEquals("dv-1", first.dataVersion());
        assertEquals("PassportFK_002_001_incomesDataAfter01052019", first.resolveDetailDataSource(2));
        assertEquals(first, second);
        verify(httpClient, times(1)).getText(contains("dokhody-detalno"));
    }

    @Test
    void loadLatestPeriodAndHelperPeriodUseExpectedDatasets() throws Exception {
        var definition = buildDefinition();
        when(httpClient.withQuery(anyString(), anyMap())).thenCallRealMethod();
        when(httpClient.getJson(contains("dsCode=ds_FK_Passport_MONTH_Periods"))).thenReturn(objectMapper.readTree("""
                {"data":[["2025-11-01T00:00:00.000Z"],["2025-12-01T00:00:00.000Z"]]}
                """));
        when(httpClient.getJson(contains("dsCode=periodHelperData"))).thenReturn(objectMapper.readTree("""
                {"data":[[2]]}
                """));

        assertEquals("2025-12-01T00:00:00.000Z", service.loadLatestPeriod(definition));
        assertEquals(2, service.loadHelperPeriod(definition, "2025-12-01T00:00:00.000Z"));
    }

    private Ogni.ODAS.iminfin.model.IminfinReportDefinition buildDefinition() {
        return new Ogni.ODAS.iminfin.model.IminfinReportDefinition(
                IminfinPassportPage.INCOMES_DETAIL,
                "report-1",
                "uuid-1",
                "version-1",
                "dv-1",
                "Доходы",
                Map.of(),
                Map.of(),
                Map.of(),
                java.util.List.of()
        );
    }
}

