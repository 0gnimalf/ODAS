package Ogni.ODAS.application.iminfin.model;

public record IminfinRegionMapping(
        Long id,
        String sourceSystem,
        String externalTerritoryCode,
        String rawTerritoryName,
        String regionCode
) {
}
