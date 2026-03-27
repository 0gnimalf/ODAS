package Ogni.ODAS.application.iminfin.model;

import java.time.OffsetDateTime;

public record IminfinReportSession(
        Long id,
        IminfinReportProfileKey profileKey,
        String reportId,
        String uuid,
        String versionLabel,
        String dataVersion,
        OffsetDateTime resolvedAt,
        IminfinDiscoveryStatus status
) {
}
