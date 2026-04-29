package Ogni.ODAS.boot.api.request;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

public record BuildMonthlySeriesRequest(
        IndicatorGroupCode groupCode,
        Long regionId,
        Long indicatorYearEntryId,
        ObservationValueKind valueKind,
        Integer year,
        Integer month,
        boolean includeQuarterAggregates,
        boolean autoCollectMissing,
        boolean forceRefresh
) {
}
