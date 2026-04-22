package Ogni.ODAS.application.dto.read;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.ObservationValueType;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.math.BigDecimal;

public record ObservationReadDto(
        Long observationId,
        Long regionId,
        String regionName,
        Long indicatorYearEntryId,
        String indicatorName,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        ObservationValueType valueType,
        BigDecimal value,
        Long datasetCollectionId
) {
}
