package Ogni.ODAS.application.dto.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public record RegionComparisonResultDto(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long indicatorYearEntryId,
        String indicatorName,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        String unitCodeLabel,
        RegionComparisonSummaryDto summary,
        List<RegionComparisonItemDto> items
) {
}
