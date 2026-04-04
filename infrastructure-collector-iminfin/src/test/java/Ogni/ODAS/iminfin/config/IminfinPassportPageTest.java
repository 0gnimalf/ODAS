package Ogni.ODAS.iminfin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinPassportPageTest {

    @Test
    void buildsPageUrlFromProperties() {
        IminfinCollectorProperties properties = new IminfinCollectorProperties();
        properties.setBaseUrl("https://www.iminfin.ru/");
        properties.setPassportRoot("areas");

        assertEquals("https://www.iminfin.ru/areas", IminfinPassportPage.PASSPORT_ROOT.pageUrl(properties));
        assertEquals("https://www.iminfin.ru/areas/dokhody-detalno", IminfinPassportPage.INCOMES_DETAIL.pageUrl(properties));
    }
}
