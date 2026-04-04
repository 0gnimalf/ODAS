package Ogni.ODAS.boot.config;

import Ogni.ODAS.application.port.out.*;
import Ogni.ODAS.application.service.AnalyzeBudgetDataService;
import Ogni.ODAS.application.service.ReferenceCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ApplicationBeansConfigTest {

    private final ApplicationBeansConfig config = new ApplicationBeansConfig();

    @Test
    void createsApplicationBeans() {
        assertInstanceOf(AnalyzeBudgetDataService.class, config.analyzeBudgetDataUseCase(
                Mockito.mock(ObservationRepositoryPort.class),
                Mockito.mock(DatasetVersionRepositoryPort.class),
                Mockito.mock(PopulationRepositoryPort.class),
                Mockito.mock(ExternalPopulationCollectorPort.class),
                Mockito.mock(ExternalSourceCollectorPort.class)
        ));
        assertInstanceOf(ReferenceCatalogService.class, config.referenceCatalogUseCase(
                Mockito.mock(ExternalReferenceCollectorPort.class),
                Mockito.mock(RegionRepositoryPort.class),
                Mockito.mock(IndicatorRepositoryPort.class)
        ));
        assertInstanceOf(ObjectMapper.class, config.objectMapper());
    }
}