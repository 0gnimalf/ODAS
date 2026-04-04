package Ogni.ODAS.db.adapter;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.mapper.ReportingPeriodEntityMapper;
import Ogni.ODAS.db.repository.JpaObservationRepository;
import Ogni.ODAS.db.support.PersistenceReferenceResolver;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.Observation;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationRepositoryAdapterTest {

    @Mock
    private JpaObservationRepository repository;
    @Mock
    private PersistenceReferenceResolver referenceResolver;

    @Test
    void saveAllCreatesNewEntityWhenObservationDoesNotExist() {
        ObservationRepositoryAdapter adapter = new ObservationRepositoryAdapter(
                repository,
                new ObservationEntityMapper(new DatasetVersionEntityMapper(), new ReportingPeriodEntityMapper()),
                referenceResolver
        );

        Observation observation = new Observation(
                null,
                new DatasetVersion(1L, "ds", "v1", SourceSystemCode.IMINFIN, OffsetDateTime.now(ZoneOffset.UTC), true),
                "77", IndicatorGroupCode.INCOME, "income/tax",
                new ReportingPeriod(null, PeriodType.MONTH, 2025, 12, null, "label"),
                ObservationValueKind.PLAN, new BigDecimal("100"), true
        );
        DatasetVersionEntity datasetVersionEntity = new DatasetVersionEntity();
        datasetVersionEntity.setId(1L);
        RegionEntity regionEntity = new RegionEntity();
        regionEntity.setCode("77");
        IndicatorEntity indicatorEntity = new IndicatorEntity();
        indicatorEntity.setCode("income/tax");
        indicatorEntity.setIndicatorGroupCode(IndicatorGroupCode.INCOME);
        ReportingPeriodEntity periodEntity = new ReportingPeriodEntity();
        periodEntity.setPeriodType(PeriodType.MONTH);
        periodEntity.setYear(2025);
        periodEntity.setMonth(12);

        when(referenceResolver.resolveDatasetVersion(any())).thenReturn(datasetVersionEntity);
        when(referenceResolver.resolveRegion("77")).thenReturn(regionEntity);
        when(referenceResolver.resolveIndicator("income/tax", IndicatorGroupCode.INCOME)).thenReturn(indicatorEntity);
        when(referenceResolver.resolveReportingPeriod(any())).thenReturn(periodEntity);
        when(repository.findByDatasetVersionIdAndRegionCodeAndIndicatorIndicatorGroupCodeAndIndicatorCodeAndReportingPeriodYearAndReportingPeriodMonthAndValueKind(
                1L, "77", IndicatorGroupCode.INCOME, "income/tax", 2025, 12, ObservationValueKind.PLAN
        )).thenReturn(Optional.empty());
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = adapter.saveAll(List.of(observation));

        assertEquals(1, saved.size());
        assertEquals("77", saved.getFirst().regionCode());
        verify(repository).saveAll(any());
    }
}
