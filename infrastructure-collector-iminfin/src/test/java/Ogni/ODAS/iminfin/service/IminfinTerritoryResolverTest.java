package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.port.out.RegionRepositoryPort;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.model.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IminfinTerritoryResolverTest {

    @Mock
    private RegionRepositoryPort regionRepositoryPort;

    @Test
    void resolvesKnownNumericCode() {
        when(regionRepositoryPort.findAll()).thenReturn(List.of(new Region(1L, "45000000", "Москва", FederalDistrictCode.CFO)));
        IminfinTerritoryResolver resolver = new IminfinTerritoryResolver(regionRepositoryPort);

        assertEquals("45000000", resolver.resolve("45000000"));
    }

    @Test
    void rejectsUnknownCode() {
        when(regionRepositoryPort.findAll()).thenReturn(List.of(new Region(1L, "45000000", "Москва", FederalDistrictCode.CFO)));
        IminfinTerritoryResolver resolver = new IminfinTerritoryResolver(regionRepositoryPort);

        assertThrows(IllegalStateException.class, () -> resolver.resolve("77000000"));
    }
}

