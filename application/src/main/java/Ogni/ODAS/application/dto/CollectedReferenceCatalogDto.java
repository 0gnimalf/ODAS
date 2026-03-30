package Ogni.ODAS.application.dto;

import java.util.List;

public record CollectedReferenceCatalogDto(
        List<CollectedRegionDto> regions,
        List<CollectedIndicatorDto> indicators
) {
}
