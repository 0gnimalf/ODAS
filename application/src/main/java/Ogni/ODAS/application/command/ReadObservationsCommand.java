package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.List;
import java.util.Set;

public record ReadObservationsCommand(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        List<Long> regionIds,
        List<Long> indicatorYearEntryIds,
        Set<ObservationValueKind> valueKinds,
        boolean includeChildren
) {
}
