package Ogni.ODAS.boot.api.request;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.List;

public record CompareRegionsRequest(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long indicatorYearEntryId,
        ObservationValueKind valueKind,
        List<Long> regionIds,
        boolean forceRefresh
) {
}
