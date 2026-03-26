package Ogni.ODAS.domain.model;

public record PopulationStat(
        Long id,
        String regionCode,
        ReportingPeriod reportingPeriod,
        Long populationValue
) {
}
