package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.dto.AnalysisResultDto;
import Ogni.ODAS.application.port.in.AnalyzeBudgetDataUseCase;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisControllerTest {

    private final AnalyzeBudgetDataUseCase useCase = mock(AnalyzeBudgetDataUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisController(useCase)).build();

    @Test
    void returnsAnalysisResults() throws Exception {
        when(useCase.analyze(any())).thenReturn(List.of(
                new AnalysisResultDto("77", IndicatorGroupCode.INCOME, "income/tax", 2025, 12, ObservationValueKind.PLAN, new BigDecimal("1"), null, null, SourceSystemCode.IMINFIN, OffsetDateTime.now(ZoneOffset.UTC), true)
        ));

        mockMvc.perform(get("/api/analysis")
                        .param("regionCode", "77")
                        .param("groupCode", "INCOME")
                        .param("indicatorCode", "income/tax")
                        .param("year", "2025")
                        .param("month", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regionCode").value("77"))
                .andExpect(jsonPath("$[0].indicatorGroupCode").value("INCOME"));
    }
}

