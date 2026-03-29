package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.model.Region;
import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import Ogni.ODAS.iminfin.http.IminfinHttpClient;
import Ogni.ODAS.iminfin.model.IminfinReportDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IminfinTerritoryResolver {

    private final IminfinCollectorProperties properties;
    private final IminfinReportDiscoveryService discoveryService;
    private final IminfinHttpClient httpClient;
    private final RegionRepositoryPort regionRepository;
    private final Set<String> validCodes = ConcurrentHashMap.newKeySet();
    private static final Object cacheInitLock = new Object();

    public IminfinTerritoryResolver(
            IminfinCollectorProperties properties,
            IminfinReportDiscoveryService discoveryService,
            IminfinHttpClient httpClient,
            RegionRepositoryPort regionRepository
    ) {
        this.properties = properties;
        this.discoveryService = discoveryService;
        this.httpClient = httpClient;
        this.regionRepository = regionRepository;
    }

    public String resolve(IminfinReportDefinition reportDefinition, String rawRegionCode) {
        if (rawRegionCode == null || rawRegionCode.isBlank())
            throw new IllegalStateException("regionCode is required");

        if (validCodes.isEmpty()) {
            synchronized (cacheInitLock) {
                if (validCodes.isEmpty()) {
                    regionRepository.findAll()
                            .forEach(region -> validCodes.add(region.code()));
                }
            }
        }

        String trimmed = rawRegionCode.trim();

        if (trimmed.matches("\\d{2,10}")) {
            if (validCodes.contains(trimmed)) return trimmed;
            Map<String, String> territories = loadTerritories(reportDefinition);
            List<String> newCodes = territories.keySet().stream()
                    .filter(code -> !validCodes.contains(code))
                    .toList();
            if (!newCodes.isEmpty()) {
                List<Region> newRegions = newCodes.stream()
                        .map(code ->
                                new Region(
                                        null,
                                        code,
                                        territories.get(code),
                                        FederalDistrictCode.NONE)
                        ).toList();
                regionRepository.saveAll(newRegions);

                validCodes.addAll(newCodes);
            }
            if (validCodes.contains(trimmed)) return trimmed;
        }
        throw new IllegalStateException("Unknown territory: " + rawRegionCode);
    }

    private Map<String, String> loadTerritories(IminfinReportDefinition reportDefinition) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("uuid", reportDefinition.uuid());
        query.put("dataVersion", reportDefinition.dataVersion());
        query.put("dsCode", "TerritoryOnlySubject");
        query.put("TERRITORIES_paramPeriod", properties.getTerritoriesParamPeriod());

        JsonNode response = httpClient.getJson(discoveryService.dataUrl(query));
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("Unexpected territory response for report " + reportDefinition.title());
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (JsonNode row : data) {
            if (!row.isArray() || row.size() < 2) {
                continue;
            }
            String code = row.get(0).asText();
            String fullName = row.get(1).asText();
            if (fullName.equals("Сириус")) continue;
            result.put(code, fullName);
//            if (row.size() > 2) {
//                result.put(IminfinTextNormalizer.normalize(row.get(2).asText()), code);
//            }
        }
        return result;
    }
}
