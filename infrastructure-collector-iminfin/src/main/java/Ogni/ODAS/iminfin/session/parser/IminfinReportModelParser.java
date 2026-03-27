package Ogni.ODAS.iminfin.session.parser;

import com.fasterxml.jackson.databind.JsonNode;
import Ogni.ODAS.iminfin.session.model.IminfinReportModelParameterDescriptor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class IminfinReportModelParser {

    private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    public Map<String, IminfinReportModelParameterDescriptor> extractParameters(JsonNode root) {
        Map<String, IminfinReportModelParameterDescriptor> result = new LinkedHashMap<>();
        walk(root, result);
        return result;
    }

    private void walk(JsonNode node, Map<String, IminfinReportModelParameterDescriptor> target) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            String parameterName = firstText(node, "parameterName", "name", "code", "ref");
            boolean looksLikeParameter = parameterName != null
                    && PARAMETER_NAME_PATTERN.matcher(parameterName).matches()
                    && (node.has("required")
                    || node.has("defaultValue")
                    || node.has("default")
                    || node.has("valueType")
                    || node.has("dataType")
                    || node.has("type"));

            if (looksLikeParameter) {
                IminfinReportModelParameterDescriptor parsed = new IminfinReportModelParameterDescriptor(
                        parameterName,
                        firstText(node, "valueType", "dataType", "type", "valueKind"),
                        node.path("required").asBoolean(false),
                        firstText(node, "defaultValue", "default", "selectedValue", "value")
                );

                target.merge(parameterName, normalize(parsed), this::merge);
            }

            node.fields().forEachRemaining(entry -> walk(entry.getValue(), target));
            return;
        }

        if (node.isArray()) {
            node.forEach(child -> walk(child, target));
        }
    }

    private IminfinReportModelParameterDescriptor normalize(IminfinReportModelParameterDescriptor descriptor) {
        return new IminfinReportModelParameterDescriptor(
                descriptor.parameterName(),
                descriptor.valueType() == null || descriptor.valueType().isBlank() ? "STRING" : descriptor.valueType(),
                descriptor.required(),
                descriptor.defaultValue()
        );
    }

    private IminfinReportModelParameterDescriptor merge(
            IminfinReportModelParameterDescriptor left,
            IminfinReportModelParameterDescriptor right
    ) {
        return new IminfinReportModelParameterDescriptor(
                left.parameterName(),
                pick(left.valueType(), right.valueType(), "STRING"),
                left.required() || right.required(),
                pick(left.defaultValue(), right.defaultValue(), null)
        );
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child == null || child.isNull()) {
                continue;
            }
            if (child.isValueNode()) {
                String value = child.asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String pick(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }
}
