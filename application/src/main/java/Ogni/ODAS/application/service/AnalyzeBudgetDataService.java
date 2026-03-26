package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.AnalysisResultDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
import Ogni.ODAS.application.port.out.DatasetVersionRepositoryPort;
import Ogni.ODAS.application.port.out.ExternalSourceCollectorPort;
import Ogni.ODAS.application.port.out.ObservationRepositoryPort;
import Ogni.ODAS.application.port.out.PopulationRepositoryPort;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.Observation;
import Ogni.ODAS.domain.model.PopulationStat;
import Ogni.ODAS.domain.model.ReportingPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class AnalyzeBudgetDataService implements AnalyzeBudgetDataUseCase {

    private final ObservationRepositoryPort observationRepositoryPort;
    private final DatasetVersionRepositoryPort datasetVersionRepositoryPort;
    private final PopulationRepositoryPort populationRepositoryPort;
    private final ExternalSourceCollectorPort externalSourceCollectorPort;

    public AnalyzeBudgetDataService(
            ObservationRepositoryPort observationRepositoryPort,
            DatasetVersionRepositoryPort datasetVersionRepositoryPort,
            PopulationRepositoryPort populationRepositoryPort,
            ExternalSourceCollectorPort externalSourceCollectorPort
    ) {
        this.observationRepositoryPort = observationRepositoryPort;
        this.datasetVersionRepositoryPort = datasetVersionRepositoryPort;
        this.populationRepositoryPort = populationRepositoryPort;
        this.externalSourceCollectorPort = externalSourceCollectorPort;
    }

    @Override
    public List<AnalysisResultDto> analyze(AnalyzeBudgetDataCommand command) {
        if (!command.forceRefresh()) {
            List<Observation> cached = observationRepositoryPort.findAllByRegionIndicatorAndPeriod(
                    command.regionCode(),
                    command.indicatorCode(),
                    command.year(),
                    command.month()
            );

            if (!cached.isEmpty()) {
                return toResult(cached, true);
            }
        }

        List<CollectedObservationDto> collected = externalSourceCollectorPort.collect(command);

        if (collected.isEmpty()) {
            throw new IllegalStateException("No data collected from external source");
        }

        DatasetVersion datasetVersion = datasetVersionRepositoryPort.save(
                new DatasetVersion(
                        null,
                        "regional-budget-execution",
                        "dynamic",
                        SourceSystemCode.IMINFIN,
                        OffsetDateTime.now(),
                        true
                )
        );

        List<Observation> observations = collected.stream()
                .map(e -> mapToObservation(e, datasetVersion))
                .toList();
        List<Observation> saved = observationRepositoryPort.saveAll(observations);

        return toResult(saved, false);
    }

    private Observation mapToObservation(CollectedObservationDto dto, DatasetVersion datasetVersion) {

        ReportingPeriod reportingPeriod = new ReportingPeriod(
                null,
                PeriodType.MONTH,
                dto.year(),
                dto.month(),
                null,
                LocalDate.of(dto.year(), dto.month(), 1),
                LocalDate.of(dto.year(), dto.month(), 1).plusMonths(1),
                dto.month() + "." + dto.year()
        );

        return new Observation(
                null,
                datasetVersion,
                dto.regionCode(),
                dto.indicatorCode(),
                reportingPeriod,
                dto.valueKind(),
                dto.value(),
                dto.cumulative()
        );
    }

    private List<AnalysisResultDto> toResult(List<Observation> observations, boolean fromCache) {
        return observations.stream()
                .map(obs -> toDto(obs, fromCache))
                .toList();
    }

    private AnalysisResultDto toDto(Observation observation, boolean fromCache) {
        BigDecimal perCapita = calculatePerCapita(observation);
        return new AnalysisResultDto(
                observation.regionCode(),
                observation.indicatorCode(),
                observation.reportingPeriod().year(),
                observation.reportingPeriod().month(),
                observation.valueKind(),
                observation.value(),
                perCapita,
                null,
                observation.datasetVersion().sourceSystem().name(),
                observation.datasetVersion().collectedAt(),
                fromCache
        );
    }

    private BigDecimal calculatePerCapita(Observation observation) {
        Optional<PopulationStat> population = populationRepositoryPort.findByRegionAndPeriod(
                observation.regionCode(),
                observation.reportingPeriod().year(),
                observation.reportingPeriod().month()
        );

        if (population.isEmpty() || population.get().populationValue() == null || population.get().populationValue() == 0) {
            return null;
        }

        return observation.value()
                .divide(BigDecimal.valueOf(population.get().populationValue()), 6, RoundingMode.HALF_UP);
    }
}
