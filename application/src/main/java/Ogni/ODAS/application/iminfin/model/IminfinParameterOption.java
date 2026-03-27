package Ogni.ODAS.application.iminfin.model;

import java.time.OffsetDateTime;

public record IminfinParameterOption(
        Long id,
        IminfinReportProfileKey profileKey,
        String parameterName,
        String externalValue,
        String rawLabel,
        String internalOptionCode,
        IminfinOptionVerificationStatus verificationStatus,
        OffsetDateTime discoveredAt
) {
}
