package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;

public record ExternalRegionRef(
        SourceSystemCode sourceSystemCode,
        String externalCode,
        String name
) {
}
