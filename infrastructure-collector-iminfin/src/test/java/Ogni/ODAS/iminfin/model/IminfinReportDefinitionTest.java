package Ogni.ODAS.iminfin.model;

import Ogni.ODAS.iminfin.config.IminfinPassportPage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IminfinReportDefinitionTest {

    @Test
    void resolvesAfterDetailDataSourceWhenHelperPeriodIsModern() {
        IminfinReportDefinition definition = new IminfinReportDefinition(
                IminfinPassportPage.INCOMES_DETAIL,
                "r", "u", "v", "dv", "title",
                Map.of("territory", new IminfinParameterDefinition("territory", "string", "45000000", false)),
                Map.of("afterDs", new IminfinDataSourceDefinition("afterDs", List.of(), List.of(), false, List.of(), null)),
                Map.of("ViewBefore", "beforeDs", "ViewAfter", "afterDs"),
                List.of()
        );

        assertEquals("afterDs", definition.resolveDetailDataSource(2));
        assertEquals("45000000", definition.defaultValue("territory"));
    }

    @Test
    void fallsBackToBeforeDataSource() {
        IminfinReportDefinition definition = new IminfinReportDefinition(
                IminfinPassportPage.INCOMES_DETAIL,
                "r", "u", "v", "dv", "title",
                Map.of(),
                Map.of("beforeDs", new IminfinDataSourceDefinition("beforeDs", List.of(), List.of(), false, List.of(), null)),
                Map.of("ViewBefore", "beforeDs"),
                List.of()
        );

        assertEquals("beforeDs", definition.resolveDetailDataSource(1));
        assertEquals("PassportFK_001_001_digestGridData", definition.resolvePopulationDataSource());
    }

    @Test
    void requireDataSourceThrowsForUnknownCode() {
        IminfinReportDefinition definition = new IminfinReportDefinition(
                IminfinPassportPage.INCOMES_DETAIL, "r", "u", "v", "dv", "title", Map.of(), Map.of(), Map.of(), List.of()
        );

        assertThrows(IllegalStateException.class, () -> definition.requireDataSource("missing"));
    }
}

