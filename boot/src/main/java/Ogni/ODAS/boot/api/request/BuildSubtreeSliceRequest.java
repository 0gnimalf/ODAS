package Ogni.ODAS.boot.api.request;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

public record BuildSubtreeSliceRequest(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long regionId,
        Long rootIndicatorYearEntryId,
        ObservationValueKind valueKind,
        boolean forceRefresh
) {
}
