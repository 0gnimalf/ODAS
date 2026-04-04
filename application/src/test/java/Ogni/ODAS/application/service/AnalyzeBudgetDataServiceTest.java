package Ogni.ODAS.application.service;

import Ogni.ODAS.application.command.AnalyzeBudgetDataCommand;
import Ogni.ODAS.application.dto.CollectedDatasetDto;
import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.application.port.out.*;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.Observation;
import Ogni.ODAS.domain.model.PopulationStat;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeBudgetDataServiceTest {

    @Mock
    private ObservationRepositoryPort observationRepositoryPort;
    @Mock
    private DatasetVersionRepositoryPort datasetVersionRepositoryPort;
    @Mock
    private PopulationRepositoryPort populationRepositoryPort;
    @Mock
    private ExternalPopulationCollectorPort externalPopulationCollectorPort;
    @Mock
    private ExternalSourceCollectorPort externalSourceCollectorPort;

    @InjectMocks
    private AnalyzeBudgetDataService service;

    private AnalyzeBudgetDataCommand command;
    private DatasetVersion datasetVersion;
    private ReportingPeriod reportingPeriodMonth;
    private ReportingPeriod reportingPeriodYear;

    @BeforeEach
    void setUp() {
        command = new AnalyzeBudgetDataCommand("45000000", IndicatorGroupCode.INCOME, "income/налоговые-и-неналоговые-доходы", 2025, 12, false);
        datasetVersion = new DatasetVersion(1L, "dataset", "v1", SourceSystemCode.IMINFIN, OffsetDateTime.now(ZoneOffset.UTC), true);
        reportingPeriodMonth = new ReportingPeriod(1L, PeriodType.MONTH, 2025, 12, null, "с 01.12.2025 по 01.01.2026");
        reportingPeriodYear = new ReportingPeriod(2L, PeriodType.YEAR, 2025, null, null, "На 01.01.2025");
    }

    @Test
    void returnsCachedObservationsWithoutCallingExternalCollector() {
        Observation observation = new Observation(
                1L, datasetVersion, command.regionCode(), command.indicatorGroupCode(), command.indicatorCode(),
                reportingPeriodMonth, ObservationValueKind.PLAN, new BigDecimal("1000"), true
        );
        when(observationRepositoryPort.findAllByRegionAndIndicatorAndPeriod(
                command.regionCode(), command.indicatorGroupCode(), command.indicatorCode(), command.year(), command.month()
        )).thenReturn(List.of(observation));
        when(populationRepositoryPort.findByRegionAndYear(command.regionCode(), command.year()))
                .thenReturn(Optional.of(new PopulationStat(1L, command.regionCode(), reportingPeriodYear, 100L)));

        var result = service.analyze(command);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().fromCache());
        assertEquals(new BigDecimal("10.000000"), result.getFirst().perCapitaValue());
        verifyNoInteractions(externalSourceCollectorPort);
    }

    @Test
    void collectsAndPersistsObservationsWhenCacheMisses() {
        when(observationRepositoryPort.findAllByRegionAndIndicatorAndPeriod(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        CollectedObservationDto requested = new CollectedObservationDto(
                command.regionCode(), command.indicatorGroupCode(), command.indicatorCode(),
                command.year(), command.month(), ObservationValueKind.PLAN, new BigDecimal("2000"), true
        );
        CollectedObservationDto unrelated = new CollectedObservationDto(
                "77000000", command.indicatorGroupCode(), command.indicatorCode(),
                command.year(), command.month(), ObservationValueKind.PLAN, new BigDecimal("3000"), true
        );
        when(externalSourceCollectorPort.collect(command))
                .thenReturn(new CollectedDatasetDto("dataset", "v1", SourceSystemCode.IMINFIN, List.of(requested, unrelated)));
        when(datasetVersionRepositoryPort.findByDatasetCodeAndVersionLabelAndSourceSystem("dataset", "v1", SourceSystemCode.IMINFIN))
                .thenReturn(Optional.of(datasetVersion));
        when(observationRepositoryPort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(populationRepositoryPort.findByRegionAndYear(command.regionCode(), command.year()))
                .thenReturn(Optional.empty());
        when(externalPopulationCollectorPort.collect(command.regionCode(), command.year()))
                .thenReturn(Optional.of(new PopulationStat(1L, command.regionCode(), reportingPeriodMonth, 200L)));
        when(populationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.analyze(command);

        assertEquals(1, result.size());
        assertFalse(result.getFirst().fromCache());
        assertEquals(new BigDecimal("10.000000"), result.getFirst().perCapitaValue());
        verify(observationRepositoryPort).saveAll(any());
    }

    @Test
    void throwsWhenExternalCollectorReturnsNoObservations() {
        when(observationRepositoryPort.findAllByRegionAndIndicatorAndPeriod(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(externalSourceCollectorPort.collect(command))
                .thenReturn(new CollectedDatasetDto("dataset", "v1", SourceSystemCode.IMINFIN, List.of()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.analyze(command));

        assertTrue(ex.getMessage().contains("No data collected"));
    }

    @Test
    void throwsWhenCollectedDatasetDoesNotContainRequestedObservation() {
        when(observationRepositoryPort.findAllByRegionAndIndicatorAndPeriod(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        CollectedObservationDto unrelated = new CollectedObservationDto(
                "77000000", command.indicatorGroupCode(), command.indicatorCode(),
                command.year(), command.month(), ObservationValueKind.PLAN, new BigDecimal("3000"), true
        );
        when(externalSourceCollectorPort.collect(command))
                .thenReturn(new CollectedDatasetDto("dataset", "v1", SourceSystemCode.IMINFIN, List.of(unrelated)));
        when(datasetVersionRepositoryPort.findByDatasetCodeAndVersionLabelAndSourceSystem(any(), any(), any()))
                .thenReturn(Optional.of(datasetVersion));
        when(observationRepositoryPort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.analyze(command));

        assertTrue(ex.getMessage().contains("Collected dataset does not contain requested observation"));
    }
}
