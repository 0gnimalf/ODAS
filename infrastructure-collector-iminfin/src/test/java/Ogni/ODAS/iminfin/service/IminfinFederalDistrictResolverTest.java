package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinFederalDistrictResolverTest {

    private final IminfinFederalDistrictResolver resolver = new IminfinFederalDistrictResolver();

    @Test
    void resolvesKnownRegion() {
        assertEquals(FederalDistrictCode.CFO, resolver.resolve("г. Москва"));
        assertEquals(FederalDistrictCode.DFO, resolver.resolve("Республика Саха (Якутия)"));
    }

    @Test
    void returnsNoneForUnknownOrSpecialRegion() {
        assertEquals(FederalDistrictCode.NONE, resolver.resolve("Неизвестный регион"));
        assertEquals(FederalDistrictCode.NONE, resolver.resolve("Сириус"));
    }
}

