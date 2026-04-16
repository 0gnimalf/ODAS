package Ogni.ODAS.db.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonSupport() {
    }

    public static JsonNode readTree(String rawJson) {
        try {
            return OBJECT_MAPPER.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse JSON payload for persistence mapping", exception);
        }
    }

    public static String write(JsonNode jsonNode) {
        try {
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize JSON payload from persistence mapping", exception);
        }
    }
}
