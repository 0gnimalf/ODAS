package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record ExternalIndicatorRow(
        IndicatorGroupCode groupCode,
        String naturalKey,
        String name,
        String parentNaturalKey,
        int level,
        int sortOrder,
        boolean hasChildren
) {
}
