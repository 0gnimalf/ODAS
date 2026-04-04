package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IminfinIndicatorTreeParserTest {

    private final IminfinIndicatorTreeParser parser = new IminfinIndicatorTreeParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesHierarchyAndMarksSections() throws Exception {
        IminfinDataSourceDefinition dataSource = new IminfinDataSourceDefinition(
                "detail", List.of(), List.of("name", "level", "plan"), false, List.of(), null
        );
        var rows = objectMapper.readTree("""
                [
                  ["Доходы",1,null],
                  ["Налоговые доходы",2,100],
                  ["Налоговые доходы",2,200]
                ]
                """);

        var result = parser.parseDetailRows("income", dataSource, rows);

        assertEquals(3, result.size());
        assertTrue(result.get(0).section());
        assertEquals("income/доходы", result.get(0).code());
        assertEquals("income/доходы/налоговые-доходы", result.get(1).code());
        assertEquals("income/доходы/налоговые-доходы-2", result.get(2).code());
        assertEquals(result.get(0).code(), result.get(1).parentCode());
    }

    @Test
    void buildCodeFallsBackWhenSlugIsBlank() {
        String code = parser.buildCode("root", null, "!!!", 5, new java.util.HashSet<>());

        assertEquals("root/indicator-5", code);
    }
}
