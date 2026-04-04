package Ogni.ODAS.iminfin.http;

import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IminfinHttpClientTest {

    @Test
    void withQuerySkipsNullsAndEncodesValues() {
        IminfinHttpClient client = new IminfinHttpClient(new ObjectMapper(), new IminfinCollectorProperties());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", "abc");
        params.put("dsCode", "my code");
        params.put("nullValue", null);

        String url = client.withQuery("https://example.org/data", params);

        assertEquals("https://example.org/data?uuid=abc&dsCode=my+code", url);
    }
}
