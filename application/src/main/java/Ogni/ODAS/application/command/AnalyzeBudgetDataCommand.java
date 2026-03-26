package Ogni.ODAS.application.command;

public record AnalyzeBudgetDataCommand(
        String regionCode,
        String indicatorCode,

        Integer year,
        Integer month,
        boolean forceRefresh
) {
}
