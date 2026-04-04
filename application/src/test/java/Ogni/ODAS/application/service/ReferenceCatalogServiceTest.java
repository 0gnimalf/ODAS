package Ogni.ODAS.application.service;

import Ogni.ODAS.application.dto.CollectedIndicatorDto;
import Ogni.ODAS.application.dto.CollectedReferenceCatalogDto;
import Ogni.ODAS.application.dto.CollectedRegionDto;
import Ogni.ODAS.application.port.out.ExternalReferenceCollectorPort;
import Ogni.ODAS.application.port.out.IndicatorRepositoryPort;
import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferenceCatalogServiceTest {

    @Mock
    private ExternalReferenceCollectorPort externalReferenceCollectorPort;
    @Mock
    private RegionRepositoryPort regionRepositoryPort;
    @Mock
    private IndicatorRepositoryPort indicatorRepositoryPort;

    @InjectMocks
    private ReferenceCatalogService service;

    @Test
    void syncPersistsRegionsIndicatorsAndReturnsCounters() {
        var regions = List.of(
                new CollectedRegionDto("77", "Москва", FederalDistrictCode.CFO),
                new CollectedRegionDto("78", "Санкт-Петербург", FederalDistrictCode.SZFO)
        );
        var indicators = List.of(
                new CollectedIndicatorDto("income", "Доходы", IndicatorGroupCode.INCOME, null, 1, 1, true),
                new CollectedIndicatorDto("income/tax", "Налоговые", IndicatorGroupCode.INCOME, "income", 2, 2, false),
                new CollectedIndicatorDto("outcome", "Расходы", IndicatorGroupCode.OUTCOME, null, 1, 3, false)
        );
        when(externalReferenceCollectorPort.collectReferenceCatalog())
                .thenReturn(new CollectedReferenceCatalogDto(regions, indicators));
        when(regionRepositoryPort.findByCode(anyString())).thenReturn(Optional.empty());
        when(regionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(indicatorRepositoryPort.findByCodeAndGroupCode(anyString(), any())).thenReturn(Optional.empty());
        when(indicatorRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.sync();

        assertEquals(2, result.regionsProcessed());
        assertEquals(3, result.indicatorsProcessed());
        assertEquals(2, result.incomeIndicators());
        assertEquals(1, result.outcomeIndicators());
        verify(indicatorRepositoryPort).save(argThat(indicator -> indicator.code().equals("income") && indicator.section()));
    }

    @Test
    void getRegionsReturnsAlphabeticallySortedList() {
        when(regionRepositoryPort.findAll()).thenReturn(List.of(
                new Region(1L, "78", "Санкт-Петербург", FederalDistrictCode.SZFO),
                new Region(2L, "77", "Москва", FederalDistrictCode.CFO)
        ));

        var result = service.getRegions();

        assertEquals(List.of("Москва", "Санкт-Петербург"), result.stream().map(Region::name).toList());
    }

    @Test
    void getIndicatorsDelegatesToRepository() {
        when(indicatorRepositoryPort.findAllByGroupCode(IndicatorGroupCode.CREDIT))
                .thenReturn(List.of(new Indicator(1L, "credit/62", "Кредит", IndicatorGroupCode.CREDIT, null, 1, 1, false)));

        var result = service.getIndicators(IndicatorGroupCode.CREDIT);

        assertEquals(1, result.size());
        assertEquals("credit/62", result.getFirst().code());
        verify(indicatorRepositoryPort).findAllByGroupCode(IndicatorGroupCode.CREDIT);
    }
}
