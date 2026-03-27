package Ogni.ODAS.application.iminfin.model;

import java.time.OffsetDateTime;

public record IminfinDiscoveryState(
        Long id,
        IminfinReportProfileKey profileKey,
        IminfinDiscoveryStatus status,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastErrorAt,
        String lastErrorMessage
) {
}
