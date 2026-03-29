package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;

import java.util.List;

public record CollectedDatasetDto(
        String datasetCode,
        String versionLabel,
        SourceSystemCode sourceSystem,
        List<CollectedObservationDto> observations
) {
}
