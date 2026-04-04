package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinObservationMapperTest {

    private final IminfinObservationMapper mapper = new IminfinObservationMapper(new IminfinIndicatorTreeParser());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsDetailRowsIntoMultipleObservationKinds() throws Exception {
        IminfinDataSourceDefinition dataSource = new IminfinDataSourceDefinition(
                "detail", List.of(), List.of("name", "level", "plan", "consFact", "factRFPercent"), false, List.of(), null
        );
        var rows = objectMapper.readTree("""
                [["Доходы",1,1000,900,105.4]]
                """);

        var result = mapper.mapDetailObservationsForRegion("45000000", IndicatorGroupCode.INCOME, 2025, 12, "income", dataSource, rows);

        assertEquals(3, result.size());
        assertEquals(ObservationValueKind.PLAN, result.get(0).valueKind());
        assertEquals(new BigDecimal("1000"), result.get(0).value());
        assertEquals(ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET, result.get(1).valueKind());
        assertEquals(ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION, result.get(2).valueKind());
    }

    @Test
    void mapsCreditRowsByNormalizedRegionName() throws Exception {
        IminfinDataSourceDefinition dataSource = new IminfinDataSourceDefinition(
                "credit", List.of(), List.of(), false, List.of(), null
        );
        var rows = objectMapper.readTree("""
                [["г. Москва",10,20,30,40,50]]
                """);

        var result = mapper.mapCreditObservationsForIndicator("credit/62", 2025, 12, dataSource, rows, Map.of("г. москва", "45000000"));

        assertEquals(5, result.size());
        assertEquals("45000000", result.getFirst().regionCode());
        assertEquals(ObservationValueKind.PLAN, result.getFirst().valueKind());
    }
}
