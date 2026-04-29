package Ogni.ODAS.boot.api.request;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.List;

public record BuildRegionIndicatorMatrixRequest(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        List<Long> regionIds,
        List<Long> indicatorYearEntryIds,
        ObservationValueKind valueKind,
        boolean forceRefresh
) {
}
