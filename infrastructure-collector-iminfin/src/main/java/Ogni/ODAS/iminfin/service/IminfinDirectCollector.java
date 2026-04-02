package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.Region;
import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
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
    private final IminfinCollectorProperties properties;
    private final RegionRepositoryPort regionRepositoryPort;

    public IminfinDirectCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinTerritoryResolver territoryResolver,
            IminfinHttpClient httpClient,
            IminfinObservationMapper observationMapper,
            IminfinCollectorProperties properties,
            RegionRepositoryPort regionRepositoryPort
    ) {
        this.discoveryService = discoveryService;
        this.territoryResolver = territoryResolver;
        this.httpClient = httpClient;
        this.observationMapper = observationMapper;
        this.properties = properties;
        this.regionRepositoryPort = regionRepositoryPort;
    }

    @Override
    public CollectedDatasetDto collect(AnalyzeBudgetDataCommand command) {
        validate(command);

        return switch (command.indicatorGroupCode()) {
            case INCOME -> collectIncome(command);
            case OUTCOME -> collectOutcome(command);
            case FIN_SOURCE -> collectFinSource(command);
            case CREDIT -> collectCredit(command);
            default ->
                    throw new IllegalStateException("Unsupported indicatorGroupCode: " + command.indicatorGroupCode());
        };
    }

    private CollectedDatasetDto collectIncome(AnalyzeBudgetDataCommand command) {
        return collectDetail(command, IminfinPassportPage.INCOMES_DETAIL, IndicatorGroupCode.INCOME, "income");
    }

    private CollectedDatasetDto collectOutcome(AnalyzeBudgetDataCommand command) {
        String prefix;
        int outcomesType;
        if (command.indicatorCode().startsWith("outcome/rzpr/")) {
            prefix = "outcome/rzpr";
            outcomesType = 2;
        } else if (command.indicatorCode().startsWith("outcome/kvr/")) {
            prefix = "outcome/kvr";
            outcomesType = 3;
        } else {
            throw new IllegalStateException("Outcome indicatorCode must start with 'outcome/rzpr/' or 'outcome/kvr/'");
        }
        return collectDetail(command, IminfinPassportPage.OUTCOMES_DETAIL, outcomesType, IndicatorGroupCode.OUTCOME, prefix);
    }

    private CollectedDatasetDto collectFinSource(AnalyzeBudgetDataCommand command) {
        return collectDetail(command, IminfinPassportPage.FIN_SOURCES_DETAIL, IndicatorGroupCode.FIN_SOURCE, "fin-source");
    }

    private CollectedDatasetDto collectDetail(
            AnalyzeBudgetDataCommand command,
            IminfinPassportPage page,
            IndicatorGroupCode indicatorGroupCode
    ) {
        return collectDetail(command, page, indicatorGroupCode, null);
    }

    private CollectedDatasetDto collectDetail(
            AnalyzeBudgetDataCommand command,
            IminfinPassportPage page,
            IndicatorGroupCode indicatorGroupCode,
            String rootPrefix
    ) {
        return collectDetail(command, page, null, indicatorGroupCode, rootPrefix);
    }

    private CollectedDatasetDto collectDetail(
            AnalyzeBudgetDataCommand command,
            IminfinPassportPage page,
            Integer outcomesType,
            IndicatorGroupCode indicatorGroupCode,
            String rootPrefix
    ) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(page);
        String territoryCode = territoryResolver.resolve(command.regionCode());
        String period = toIminfinPeriod(command.year(), command.month());
        int helperPeriod = discoveryService.loadHelperPeriod(reportDefinition, period);
        String dsCode = reportDefinition.resolveDetailDataSource(helperPeriod);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", dsCode);
        query.put("territory", territoryCode);
        query.put("paramPeriod", period);
        if (outcomesType != null) {
            query.put("PassportFK_002_002_outcomesType", outcomesType);
        }

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode dataRows = response.path("data");
        IminfinDataSourceDefinition dataSource = reportDefinition.requireDataSource(dsCode);
        List<CollectedObservationDto> observations = observationMapper.mapDetailObservationsForRegion(
                territoryCode,
                indicatorGroupCode,
                command.year(),
                command.month(),
                rootPrefix,
                dataSource,
                dataRows
        );

        if (observations.isEmpty()) {
            throw new IllegalStateException("No observations found for indicatorCode=" + command.indicatorCode());
        }

        return new CollectedDatasetDto(
                reportDefinition.page().name().toLowerCase(),
                reportDefinition.dataVersion(),
                SourceSystemCode.IMINFIN,
                observations
        );
    }

    private CollectedDatasetDto collectCredit(AnalyzeBudgetDataCommand command) {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        String period = toIminfinPeriod(command.year(), command.month());
        Region region = regionRepositoryPort.findByCode(command.regionCode())
                .orElseThrow(() -> new IllegalStateException("Region not found in local reference catalog: " + command.regionCode()));

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "PassportFK_001_005_creditGridData");
        query.put("PassportFK_001_005_paramCredits", toCreditSourceCode(command.indicatorCode()));
        query.put("paramPeriod", period);

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode dataRows = response.path("data");
        List<CollectedObservationDto> observations = observationMapper.mapCreditObservationsForIndicator(
                command.indicatorCode(),
                command.year(),
                command.month(),
                reportDefinition.requireDataSource("PassportFK_001_005_creditGridData"),
                dataRows,
                regionCodeByNormalizedName()
        );

        if (observations.isEmpty()) {
            throw new IllegalStateException(
                    "No credit observations found for region=" + command.regionCode() + ", indicatorCode=" + command.indicatorCode()
            );
        }

        return new CollectedDatasetDto(
                reportDefinition.page().name().toLowerCase(),
                reportDefinition.dataVersion(),
                SourceSystemCode.IMINFIN,
                observations
        );
    }

    private Map<String, String> regionCodeByNormalizedName() {
        Map<String, String> result = new LinkedHashMap<>();
        for (var region : regionRepositoryPort.findAll()) {
            if (region.name() == null || region.name().isBlank()) {
                continue;
            }
            result.putIfAbsent(IminfinTextNormalizer.normalize(region.name()), region.code());
        }
        return result;
    }

    private String toCreditSourceCode(String indicatorCode) {
        if (indicatorCode == null || !indicatorCode.startsWith("credit/")) {
            throw new IllegalStateException("Credit indicatorCode must start with 'credit/'");
        }
        return indicatorCode.substring("credit/".length());
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
        if (command.indicatorGroupCode() == null) {
            throw new IllegalStateException("indicatorGroupCode is required");
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
        if (command.year() < properties.getMinYearToCollect() || command.year() > properties.getMaxYearToCollect()) {
            throw new IllegalStateException("year must be valid");
        }
    }
}
