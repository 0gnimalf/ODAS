package Ogni.ODAS.iminfin.session.model;

import com.fasterxml.jackson.databind.JsonNode;

public record IminfinReportModels(
        JsonNode primary,
        JsonNode meta
) {
}
