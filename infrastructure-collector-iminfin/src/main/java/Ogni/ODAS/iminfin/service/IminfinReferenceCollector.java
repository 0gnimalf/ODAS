package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedReferenceCatalogDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinReferenceCollector implements ExternalReferenceCollectorPort {

    private static final DateTimeFormatter IMINFIN_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinHttpClient httpClient;
    private final IminfinIndicatorTreeParser indicatorTreeParser;
    private final IminfinFederalDistrictResolver federalDistrictResolver;

    public IminfinReferenceCollector(
            IminfinReportDiscoveryService discoveryService,
            IminfinHttpClient httpClient,
            IminfinIndicatorTreeParser indicatorTreeParser,
            IminfinFederalDistrictResolver federalDistrictResolver
    ) {
        this.discoveryService = discoveryService;
        this.httpClient = httpClient;
        this.indicatorTreeParser = indicatorTreeParser;
        this.federalDistrictResolver = federalDistrictResolver;
    }

    @Override
    public CollectedReferenceCatalogDto collectReferenceCatalog() {
        List<CollectedRegionDto> regions = collectRegions();
        List<CollectedIndicatorDto> indicators = new ArrayList<>();
        indicators.addAll(collectIncomeIndicators());
        indicators.addAll(collectOutcomeIndicators());
        indicators.addAll(collectCreditIndicators());
        indicators.addAll(collectFinSourceIndicators());
        return new CollectedReferenceCatalogDto(regions, indicators);
    }

    private List<CollectedRegionDto> collectRegions() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.INCOMES_DETAIL);
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "TerritoryOnlySubject");
        query.put("TERRITORIES_paramPeriod", "2014-05-28T00:00:00.000Z");

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("Unexpected territory response for report " + reportDefinition.title());
        }

        Map<String, String> regions = new LinkedHashMap<>();
        for (JsonNode row : data) {
            if (!row.isArray() || row.size() < 2) {
                continue;
            }
            String code = row.get(0).asText();
            String fullName = row.get(1).asText();
            regions.put(code, fullName);
        }
        return regions.entrySet().stream()
                .map(entry -> new CollectedRegionDto(
                        entry.getKey(),
                        entry.getValue(),
                        federalDistrictResolver.resolve(entry.getValue())
                ))
                .toList();
    }

    private List<CollectedIndicatorDto> collectIncomeIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.INCOMES_DETAIL);
        String period = discoveryService.loadLatestPeriod(reportDefinition);
        String dsCode = reportDefinition.resolveDetailDataSource(discoveryService.loadHelperPeriod(reportDefinition, period));
        JsonNode data = loadDetailData(reportDefinition, dsCode, reportDefinition.defaultValue("territory"), period, null);
        return indicatorTreeParser.parseDetailRows("income", reportDefinition.requireDataSource(dsCode), data).stream()
                .map(row -> new CollectedIndicatorDto(
                        row.code(),
                        row.caption(),
                        IndicatorGroupCode.INCOME,
                        row.parentCode(),
                        row.level(),
                        row.sortOrder(),
                        row.section()
                ))
                .toList();
    }

    private List<CollectedIndicatorDto> collectOutcomeIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.OUTCOMES_DETAIL);
        String period = discoveryService.loadLatestPeriod(reportDefinition);
        List<CollectedIndicatorDto> result = new ArrayList<>();

        IminfinDataSourceDefinition types = reportDefinition.requireDataSource("PassportFK_002_002_outcomesTypesFix");
        for (List<String> row : types.fixedData()) {
            if (row.size() < 2) {
                continue;
            }
            String caption = row.get(0);
            String code = row.get(1);
            String prefix = switch (code) {
                case "2" -> "outcome/rzpr";
                case "3" -> "outcome/kvr";
                default -> null;
            };
            if (prefix == null) {
                continue;
            }

            String dsCode = reportDefinition.resolveDetailDataSource(discoveryService.loadHelperPeriod(reportDefinition, period));
            JsonNode data = loadDetailData(reportDefinition, dsCode, reportDefinition.defaultValue("territory"), period, Integer.valueOf(code));
            result.addAll(indicatorTreeParser.parseDetailRows(prefix, reportDefinition.requireDataSource(dsCode), data).stream()
                    .map(parsed -> new CollectedIndicatorDto(
                            parsed.code(),
                            parsed.caption(),
                            IndicatorGroupCode.OUTCOME,
                            parsed.parentCode(),
                            parsed.level(),
                            parsed.sortOrder(),
                            parsed.section()
                    ))
                    .toList());
        }

        return result;
    }

    private List<CollectedIndicatorDto> collectCreditIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.CREDITS_COMPARE);
        IminfinDataSourceDefinition dataSource = reportDefinition.requireDataSource("PassportFK_001_005_paramCreditsData_fixed");
        List<CollectedIndicatorDto> result = new ArrayList<>();
        String currentParentCode = null;
        int sortOrder = 0;

        for (List<String> row : dataSource.fixedData()) {
            if (row.size() < 2) {
                continue;
            }
            String caption = row.get(0);
            String sourceCode = row.get(1);
            String code = "credit/" + sourceCode;
            boolean child = caption != null && caption.trim().startsWith("-");
            String cleanCaption = child ? caption.trim().substring(1).trim() : caption.trim();
            if (!child) {
                currentParentCode = code;
            }
            sortOrder++;
            result.add(new CollectedIndicatorDto(
                    code,
                    cleanCaption,
                    IndicatorGroupCode.CREDIT,
                    child ? currentParentCode : null,
                    child ? 2 : 1,
                    sortOrder,
                    false
            ));
        }
        return result;
    }

    private List<CollectedIndicatorDto> collectFinSourceIndicators() {
        IminfinReportDefinition reportDefinition = discoveryService.discover(IminfinPassportPage.FIN_SOURCES_DETAIL);
        String period = discoveryService.loadLatestPeriod(reportDefinition);
        String dsCode = reportDefinition.resolveDetailDataSource(discoveryService.loadHelperPeriod(reportDefinition, period));
        JsonNode data = loadDetailData(reportDefinition, dsCode, reportDefinition.defaultValue("territory"), period, null);
        return indicatorTreeParser.parseDetailRows("fin-source", reportDefinition.requireDataSource(dsCode), data).stream()
                .map(row -> new CollectedIndicatorDto(
                        row.code(),
                        row.caption(),
                        IndicatorGroupCode.FIN_SOURCE,
                        row.parentCode(),
                        row.level(),
                        row.sortOrder(),
                        row.section()
                ))
                .toList();
    }

    private JsonNode loadDetailData(
            IminfinReportDefinition reportDefinition,
            String dsCode,
            String territoryCode,
            String period,
            Integer outcomesType
    ) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", dsCode);
        query.put("territory", territoryCode);
        query.put("paramPeriod", period);
        if (outcomesType != null) {
            query.put("PassportFK_002_002_outcomesType", outcomesType);
        }

        return httpClient.getJson(discoveryService.dataUrl(query)).path("data");
    }

    private String toIminfinPeriod(int year, int month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .format(IMINFIN_PERIOD_FORMAT);
    }
}
