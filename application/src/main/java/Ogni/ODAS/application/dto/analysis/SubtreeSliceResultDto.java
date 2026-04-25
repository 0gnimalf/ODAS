package Ogni.ODAS.application.dto.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public record SubtreeSliceResultDto(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long regionId,
        String regionName,
        Long rootIndicatorYearEntryId,
        String rootIndicatorName,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        String unitCodeLabel,
        List<SubtreeSliceNodeDto> nodes
) {
}
