package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinDirectCollector implements ExternalSourceCollectorPort {

    private static final DateTimeFormatter IMINFIN_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinTerritoryResolver territoryResolver;
    private final IminfinHttpClient httpClient;
    private final IminfinObservationMapper observationMapper;
    private final IminfinIndicatorSelector indicatorSelector;
    private final IminfinCollectorProperties properties;

    public IminfinDirectCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinTerritoryResolver territoryResolver,
            IminfinHttpClient httpClient,
            IminfinObservationMapper observationMapper,
            IminfinIndicatorSelector indicatorSelector,
            IminfinCollectorProperties properties
    ) {
        this.discoveryService = discoveryService;
        this.territoryResolver = territoryResolver;
        this.httpClient = httpClient;
        this.observationMapper = observationMapper;
        this.indicatorSelector = indicatorSelector;
        this.properties = properties;
    }

    @Override
    public CollectedDatasetDto collect(AnalyzeBudgetDataCommand command) {
        validate(command);

        IminfinIndicatorSelector.Selection selection = indicatorSelector.parse(command.indicatorCode());
        for (IminfinPassportPage page : selection.candidatePages()) {
            CollectedDatasetDto dataset = tryCollectFromPage(command, selection, page);
            if (dataset != null && !dataset.observations().isEmpty()) {
                return dataset;
            }
        }

        throw new IllegalStateException(
                "Indicator was not found in iMinfin reports for region="
                        + command.regionCode()
                        + ", indicator="
                        + command.indicatorCode()
                        + ", period="
                        + command.month()
                        + "."
                        + command.year()
        );
    }

    @Override
    public List<Integer> getDesiredObservationIndexes() {
        return observationMapper.getDesiredObservationIndexes();
    }

    private CollectedDatasetDto tryCollectFromPage(
            AnalyzeBudgetDataCommand command,
            IminfinIndicatorSelector.Selection selection,
            IminfinPassportPage page
    ) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(page);
        String territoryCode = territoryResolver.resolve(reportDefinition, command.regionCode());
        String period = toIminfinPeriod(command.year(), command.month());
        int helperPeriod = loadHelperPeriod(reportDefinition, period);
        String dsCode = reportDefinition.resolveDetailDataSource(helperPeriod);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", dsCode);
        query.put("territory", territoryCode);
        query.put("paramPeriod", period);
        if (page == IminfinPassportPage.OUTCOMES_DETAIL) {
            query.put("PassportFK_002_002_outcomesType", selection.expensesBySection() ? 2 : 3);
        }

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode dataRows = response.path("data");
        List<CollectedObservationDto> observations = observationMapper.mapDetailObservations(
                territoryCode,
                selection.indicatorName(),
                command.year(),
                command.month(),
                reportDefinition.requireDataSource(dsCode),
                dataRows,
                indicatorSelector
        );

        if (observations.isEmpty()) {
            return null;
        }

        return new CollectedDatasetDto(
                reportDefinition.page().name().toLowerCase(),
                reportDefinition.dataVersion(),
                SourceSystemCode.IMINFIN,
                observations
        );
    }

    private int loadHelperPeriod(IminfinReportDefinition reportDefinition, String period) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "periodHelperData");
        query.put("paramPeriod", period);

        JsonNode helperResponse = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode data = helperResponse.path("data");
        if (!data.isArray() || data.isEmpty() || !data.get(0).isArray() || data.get(0).isEmpty()) {
            return 1;
        }
        return data.get(0).get(0).asInt(1);
    }

    private String toIminfinPeriod(int year, int month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .format(IMINFIN_PERIOD_FORMAT);
    }

    private void validate(AnalyzeBudgetDataCommand command) {
        if (command.regionCode() == null || command.regionCode().isBlank()) {
            throw new IllegalStateException("regionCode is required");
        }
        if (command.indicatorCode() == null || command.indicatorCode().isBlank()) {
            throw new IllegalStateException("indicatorCode is required");
        }
        if (command.year() == null || command.month() == null) {
            throw new IllegalStateException("year and month are required");
        }
        if (command.month() < 1 || command.month() > 12) {
            throw new IllegalStateException("month must be between 1 and 12");
        }
        if (command.year() < properties.getMinYearToCollect() ||
                command.year() > properties.getMaxYearToCollect()) {
            throw new IllegalStateException("year must be valid");
        }
    }
}
