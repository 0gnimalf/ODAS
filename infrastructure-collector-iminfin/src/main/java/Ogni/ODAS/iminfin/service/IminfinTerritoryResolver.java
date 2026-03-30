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

    private final RegionRepositoryPort regionRepository;
    private final Set<String> validCodes = ConcurrentHashMap.newKeySet();

    public IminfinTerritoryResolver(
            RegionRepositoryPort regionRepository
    ) {
        this.regionRepository = regionRepository;
    }

    public String resolve(String rawRegionCode) {
        if (rawRegionCode == null || rawRegionCode.isBlank()) {
            throw new IllegalStateException("regionCode is required");
        }

        ensureCacheLoaded();
        String trimmed = rawRegionCode.trim();

        if (trimmed.matches("\\d{2,10}")) {
            if (validCodes.contains(trimmed)) return trimmed;
        }
        throw new IllegalStateException("Unknown territory: " + rawRegionCode);
    }

    private void ensureCacheLoaded() {
        if (!validCodes.isEmpty()) return;
        regionRepository.findAll()
                .forEach(region -> validCodes.add(region.code()));
    }
}
