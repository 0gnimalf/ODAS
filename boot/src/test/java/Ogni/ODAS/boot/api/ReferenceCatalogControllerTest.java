package Ogni.ODAS.boot.api;

import Ogni.ODAS.application.dto.ReferenceSyncResultDto;
import Ogni.ODAS.application.port.in.ReferenceCatalogUseCase;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.Region;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceCatalogControllerTest {

    private final ReferenceCatalogUseCase useCase = mock(ReferenceCatalogUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ReferenceCatalogController(useCase)).build();

    @Test
    void syncEndpointReturnsSyncResult() throws Exception {
        when(useCase.sync()).thenReturn(new ReferenceSyncResultDto(1, 2, 3, 4, 5, 6));

        mockMvc.perform(get("/api/references/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionsProcessed").value(1))
                .andExpect(jsonPath("$.finSourceIndicators").value(6));
    }

    @Test
    void regionsAndIndicatorsEndpointsReturnPayload() throws Exception {
        when(useCase.getRegions()).thenReturn(List.of(new Region(1L, "77", "Москва", FederalDistrictCode.CFO)));
        when(useCase.getIndicators(IndicatorGroupCode.INCOME)).thenReturn(List.of(new Indicator(1L, "income/tax", "Налоговые", IndicatorGroupCode.INCOME, null, 1, 1, false)));

        mockMvc.perform(get("/api/references/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("77"));

        mockMvc.perform(get("/api/references/indicators").param("groupCode", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("income/tax"));
    }
}

