package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.port.out.ExternalPopulationCollectorPort;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.PopulationStat;
import Ogni.ODAS.domain.model.ReportingPeriod;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class IminfinPopulationCollector implements ExternalPopulationCollectorPort {

    private static final DateTimeFormatter IMINFIN_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinHttpClient httpClient;

    public IminfinPopulationCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinHttpClient httpClient
    ) {
        this.discoveryService = discoveryService;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<PopulationStat> collect(String regionCode, Integer year) {
        if (regionCode == null || regionCode.isBlank() || year == null) {
            return Optional.empty();
        }

        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.PASSPORT_ROOT);
        String dsCode = reportDefinition.resolvePopulationDataSource();
        String period = toIminfinPeriod(year, 1);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", dsCode);
        query.put("territory", regionCode);
        query.put("paramPeriod", period);

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode dataRows = response.path("data");
        if (!dataRows.isArray() || dataRows.isEmpty()) {
            return Optional.empty();
        }

        IminfinDataSourceDefinition dataSourceDefinition = reportDefinition.dataSources().get(dsCode);
        Long population = extractPopulationValue(dataSourceDefinition, dataRows);
        if (population == null || population <= 0) {
            return Optional.empty();
        }

        return Optional.of(new PopulationStat(
                null,
                regionCode,
                new ReportingPeriod(
                        null,
                        PeriodType.YEAR,
                        year,
                        null,
                        null,
                        "На 01.01." + year
                ),
                population
        ));
    }

    private Long extractPopulationValue(IminfinDataSourceDefinition dataSourceDefinition, JsonNode dataRows) {
        Map<String, Integer> columns = columnIndexes(dataSourceDefinition == null ? List.of() : dataSourceDefinition.columnNames());
        Integer nameIndex = columns.get("name");

        for (JsonNode row : dataRows) {
            if (!row.isArray() || row.isEmpty()) {
                continue;
            }
            String label = textCell(row, nameIndex);
            String normalizedLabel = IminfinTextNormalizer.normalize(label);

            if (!normalizedLabel.isBlank() && normalizedLabel.equals("численность населения (чел.)")) {
                Long value = longCell(row, columns);
                if (value == null) {
                    continue;
                }
                return value;
            }
        }
        return null;
    }

    private Long longCell(JsonNode row, Map<String, Integer> columns) {
            Integer index = columns.get("prevYearFact");
            if (index != null) {
                BigDecimal value = decimalCell(row, index);
                if (value != null) {
                    return value.longValue();
                }
            }
            return null;
    }

    private Map<String, Integer> columnIndexes(List<String> columnNames) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            result.put(columnNames.get(i), i);
        }
        return result;
    }

    private String textCell(JsonNode row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        JsonNode node = row.get(index);
        return node == null || node.isNull() ? null : node.asText();
    }

    private BigDecimal decimalCell(JsonNode row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        JsonNode node = row.get(index);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replace(" ", "").replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toIminfinPeriod(int year, int month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .format(IMINFIN_PERIOD_FORMAT);
    }
}
