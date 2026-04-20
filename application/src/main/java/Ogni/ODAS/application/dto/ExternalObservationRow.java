package Ogni.ODAS.application.dto;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;

import java.math.BigDecimal;

public record ExternalObservationRow(
        SourceSystemCode sourceSystemCode,
        String regionExternalCode,
        IndicatorGroupCode groupCode,
        String indicatorName,
        String parentIndicatorName,
        ObservationValueKind valueKind,
        BigDecimal value
) {
}
