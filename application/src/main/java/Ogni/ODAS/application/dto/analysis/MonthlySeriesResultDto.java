package Ogni.ODAS.application.dto.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public record MonthlySeriesResultDto(
        IndicatorGroupCode groupCode,
        Long regionId,
        String regionName,
        Long indicatorYearEntryId,
        String indicatorName,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        String unitCodeLabel,
        NonCumulativeValueMode nonCumulativeMode,
        Integer targetYear,
        Integer targetMonth,
        int expectedMonthCount,
        int availableMonthCount,
        boolean autoCollectedMissing,
        List<MonthlySeriesPointDto> points,
        List<QuarterAggregateDto> quarterAggregates
) {
}
