package Ogni.ODAS.db.support;

import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.entity.IndicatorEntity;
import Ogni.ODAS.db.entity.RegionEntity;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.mapper.ReportingPeriodEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.db.repository.JpaIndicatorRepository;
import Ogni.ODAS.db.repository.JpaRegionRepository;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceReferenceResolverTest {

    @Mock
    private JpaRegionRepository regionRepository;
    @Mock
    private JpaIndicatorRepository indicatorRepository;
    @Mock
    private JpaReportingPeriodRepository reportingPeriodRepository;
    @Mock
    private JpaDatasetVersionRepository datasetVersionRepository;

    private PersistenceReferenceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PersistenceReferenceResolver(
                regionRepository,
                indicatorRepository,
                reportingPeriodRepository,
                new ReportingPeriodEntityMapper(),
                datasetVersionRepository,
                new DatasetVersionEntityMapper()
        );
    }

    @Test
    void createsPlaceholderRegionAndIndicatorWhenReferenceMissing() {
        when(regionRepository.findByCode("77")).thenReturn(Optional.empty());
        when(regionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(indicatorRepository.findByCodeAndIndicatorGroupCode("income/tax", IndicatorGroupCode.INCOME)).thenReturn(Optional.empty());
        when(indicatorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegionEntity region = resolver.resolveRegion("77");
        IndicatorEntity indicator = resolver.resolveIndicator("income/tax", IndicatorGroupCode.INCOME);

        assertEquals("77", region.getCode());
        assertEquals("Заглушка|77", region.getName());
        assertEquals("income/tax", indicator.getCode());
        assertEquals("Заглушка|income/tax", indicator.getName());
    }

    @Test
    void resolvesDatasetVersionAndPeriod() {
        when(datasetVersionRepository.findById(1L)).thenReturn(Optional.empty());
        when(datasetVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportingPeriodRepository.findByPeriodTypeAndYearAndMonthAndQuarter(PeriodType.MONTH, 2025, 12, null)).thenReturn(Optional.empty());
        when(reportingPeriodRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DatasetVersionEntity version = resolver.resolveDatasetVersion(new DatasetVersion(1L, "ds", "v1", SourceSystemCode.IMINFIN, OffsetDateTime.now(ZoneOffset.UTC), true));
        var period = resolver.resolveReportingPeriod(new ReportingPeriod(1L, PeriodType.MONTH, 2025, 12, null, "label"));

        assertEquals("ds", version.getDatasetCode());
        assertEquals(2025, period.getYear());
    }
}

