package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.AnalysisResultDto;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
import Ogni.ODAS.application.port.out.*;
import Ogni.ODAS.domain.enumtype.ObservationValueType;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.Observation;
import Ogni.ODAS.domain.model.PopulationStat;
import Ogni.ODAS.domain.model.ReportingPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class AnalyzeBudgetDataService implements AnalyzeBudgetDataUseCase {

    private final ObservationRepositoryPort observationRepositoryPort;
    private final DatasetVersionRepositoryPort datasetVersionRepositoryPort;
    private final PopulationRepositoryPort populationRepositoryPort;
    private final ExternalPopulationCollectorPort externalPopulationCollectorPort;
    private final ExternalSourceCollectorPort externalSourceCollectorPort;

    public AnalyzeBudgetDataService(
            ObservationRepositoryPort observationRepositoryPort,
            DatasetVersionRepositoryPort datasetVersionRepositoryPort,
            PopulationRepositoryPort populationRepositoryPort,
            ExternalPopulationCollectorPort externalPopulationCollectorPort,
            ExternalSourceCollectorPort externalSourceCollectorPort
    ) {
        this.observationRepositoryPort = observationRepositoryPort;
        this.datasetVersionRepositoryPort = datasetVersionRepositoryPort;
        this.populationRepositoryPort = populationRepositoryPort;
        this.externalPopulationCollectorPort = externalPopulationCollectorPort;
        this.externalSourceCollectorPort = externalSourceCollectorPort;
    }

    @Override
    public List<AnalysisResultDto> analyze(AnalyzeBudgetDataCommand command) {
        if (!command.forceRefresh()) {
            List<Observation> cached = observationRepositoryPort.findAllByRegionAndIndicatorAndPeriod(
                    command.regionCode(),
                    command.indicatorGroupCode(),
                    command.indicatorCode(),
                    command.year(),
                    command.month()
            );

            if (!cached.isEmpty()) {
                return toResult(cached, true);
            }
        }

        CollectedDatasetDto collectedDataset = externalSourceCollectorPort.collect(command);

        if (collectedDataset == null || collectedDataset.observations() == null || collectedDataset.observations().isEmpty()) {
            throw new IllegalStateException("No data collected from external source");
        }

        DatasetVersion datasetVersion = resolveDatasetVersion(collectedDataset);

        List<Observation> observations = collectedDataset.observations().stream()
                .map(e -> mapToObservation(e, datasetVersion))
                .toList();
        List<Observation> saved = observationRepositoryPort.saveAll(observations);
        List<Observation> requested = filterRequestedObservations(saved, command);
        if (requested.isEmpty()) {
            throw new IllegalStateException(
                    "Collected dataset does not contain requested observation: regionCode="
                            + command.regionCode()
                            + ", indicatorGroupCode=" + command.indicatorGroupCode()
                            + ", indicatorCode=" + command.indicatorCode()
                            + ", year=" + command.year()
                            + ", month=" + command.month()
            );
        }

        return toResult(requested, false);
    }

    private List<Observation> filterRequestedObservations(List<Observation> observations, AnalyzeBudgetDataCommand command) {
        return observations.stream()
                .filter(obs -> command.regionCode().equals(obs.regionCode()))
                .filter(obs -> command.indicatorGroupCode() == obs.indicatorGroupCode())
                .filter(obs -> command.indicatorCode().equals(obs.indicatorCode()))
                .filter(obs -> command.year().equals(obs.reportingPeriod().year()))
                .filter(obs -> command.month().equals(obs.reportingPeriod().month()))
                .toList();
    }

    private DatasetVersion resolveDatasetVersion(CollectedDatasetDto collectedDataset) {
        return datasetVersionRepositoryPort.findByDatasetCodeAndVersionLabelAndSourceSystem(
                        collectedDataset.datasetCode(),
                        collectedDataset.versionLabel(),
                        collectedDataset.sourceSystem()
                )
                .orElseGet(() -> datasetVersionRepositoryPort.save(
                        new DatasetVersion(
                                null,
                                collectedDataset.datasetCode(),
                                collectedDataset.versionLabel(),
                                collectedDataset.sourceSystem(),
                                OffsetDateTime.now(ZoneOffset.UTC),
                                true
                        )
                ));
    }

    private Observation mapToObservation(CollectedObservationDto dto, DatasetVersion datasetVersion) {
        LocalDate nextMonth = LocalDate.of(dto.year(), dto.month(), 1).plusMonths(1);

        ReportingPeriod reportingPeriod = new ReportingPeriod(
                null,
                PeriodType.MONTH,
                dto.year(),
                dto.month(),
                null,
                String.format("с %02d.%02d.%04d по %02d.%02d.%04d",
                        1,
                        dto.month(),
                        dto.year(),
                        1,
                        nextMonth.getMonth().getValue(),
                        nextMonth.getYear())
        );

        return new Observation(
                null,
                datasetVersion,
                dto.regionCode(),
                dto.indicatorGroupCode(),
                dto.indicatorCode(),
                reportingPeriod,
                dto.valueKind(),
                dto.value(),
                dto.cumulative()
        );
    }

    private List<AnalysisResultDto> toResult(List<Observation> observations, boolean fromCache) {
        Observation observation = observations.getFirst();
        Optional<PopulationStat> population = populationRepositoryPort.findByRegionAndYear(
                observation.regionCode(),
                observation.reportingPeriod().year()
        );

        if (population.isEmpty()) {
            population = externalPopulationCollectorPort.collect(
                    observation.regionCode(),
                    observation.reportingPeriod().year()
            ).map(populationRepositoryPort::save);
        }

        if (population.isEmpty() || population.get().populationValue() == null || population.get().populationValue() == 0) {
            return null;
        }
        Long populationValue = population.get().populationValue();
        return observations.stream()
                .map(obs -> toDto(obs, populationValue, fromCache))
                .toList();
    }

    private AnalysisResultDto toDto(Observation observation, Long populationValue, boolean fromCache) {
        BigDecimal perCapita = calculatePerCapita(observation, populationValue);
        return new AnalysisResultDto(
                observation.regionCode(),
                observation.indicatorGroupCode(),
                observation.indicatorCode(),
                observation.reportingPeriod().year(),
                observation.reportingPeriod().month(),
                observation.valueKind(),
                observation.value(),
                perCapita,
                null,
                observation.datasetVersion().sourceSystem(),
                observation.datasetVersion().collectedAt(),
                fromCache
        );
    }

    private BigDecimal calculatePerCapita(Observation observation, Long populationValue) {
        if (!isPerCapitaApplicable(observation)) {
            return null;
        }

        return observation.value()
                .divide(BigDecimal.valueOf(populationValue), 6, RoundingMode.HALF_UP);
    }

    private boolean isPerCapitaApplicable(Observation observation) {
        return observation != null
                && observation.value() != null
                && observation.valueKind() != null
                && observation.valueKind().getObservationValueType() == ObservationValueType.ABSOLUTE;
    }
}
