package Ogni.ODAS.db.mapper;

import Ogni.ODAS.db.entity.*;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.domain.model.DatasetVersion;
import Ogni.ODAS.domain.model.Observation;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationEntityMapperTest {

    private final ObservationEntityMapper mapper = new ObservationEntityMapper(new DatasetVersionEntityMapper(), new ReportingPeriodEntityMapper());

    @Test
    void mapsEntityToDomain() {
        DatasetVersionEntity datasetVersion = new DatasetVersionEntity();
        datasetVersion.setId(1L);
        datasetVersion.setDatasetCode("ds");
        datasetVersion.setVersionLabel("v1");
        datasetVersion.setSourceSystem(SourceSystemCode.IMINFIN);
        datasetVersion.setCollectedAt(OffsetDateTime.now(ZoneOffset.UTC));

        RegionEntity region = new RegionEntity();
        region.setCode("77");

        IndicatorEntity indicator = new IndicatorEntity();
        indicator.setCode("income/tax");
        indicator.setIndicatorGroupCode(IndicatorGroupCode.INCOME);

        ReportingPeriodEntity period = new ReportingPeriodEntity();
        period.setPeriodType(PeriodType.MONTH);
        period.setYear(2025);
        period.setMonth(12);

        ObservationEntity entity = new ObservationEntity();
        entity.setId(10L);
        entity.setDatasetVersion(datasetVersion);
        entity.setRegion(region);
        entity.setIndicator(indicator);
        entity.setReportingPeriod(period);
        entity.setValueKind(ObservationValueKind.PLAN);
        entity.setValue(new BigDecimal("15"));
        entity.setCumulative(true);

        Observation domain = mapper.toDomain(entity);

        assertEquals("77", domain.regionCode());
        assertEquals("income/tax", domain.indicatorCode());
        assertEquals(new BigDecimal("15"), domain.value());
        assertTrue(domain.cumulative());
    }

    @Test
    void copiesDomainToEntity() {
        Observation domain = new Observation(
                1L,
                new DatasetVersion(1L, "ds", "v1", SourceSystemCode.IMINFIN, OffsetDateTime.now(ZoneOffset.UTC), true),
                "77", IndicatorGroupCode.INCOME, "income/tax",
                new ReportingPeriod(1L, PeriodType.MONTH, 2025, 12, null, "label"),
                ObservationValueKind.PLAN, new BigDecimal("99"), true
        );
        ObservationEntity entity = mapper.toNewEntity(domain, new DatasetVersionEntity(), new RegionEntity(), new IndicatorEntity(), new ReportingPeriodEntity());

        assertEquals(ObservationValueKind.PLAN, entity.getValueKind());
        assertEquals(new BigDecimal("99"), entity.getValue());
    }
}

