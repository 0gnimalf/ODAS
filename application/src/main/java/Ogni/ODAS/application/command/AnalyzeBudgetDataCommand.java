package Ogni.ODAS.application.command;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

public record AnalyzeBudgetDataCommand(
        String regionCode,
        IndicatorGroupCode indicatorGroupCode,
        String indicatorCode,

        Integer year,
        Integer month,
        boolean forceRefresh
) {
}
