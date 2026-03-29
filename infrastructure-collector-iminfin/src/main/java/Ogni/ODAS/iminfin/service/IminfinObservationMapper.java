package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinObservationMapper {

    @Getter
    public List<Integer> desiredObservationIndexes;

    public List<CollectedObservationDto> mapDetailObservations(
            String regionCode,
            String requestedIndicator,
            int year,
            int month,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows,
            IminfinIndicatorSelector indicatorSelector
    ) {
        if (!dataRows.isArray()) {
            throw new IllegalStateException("Unexpected iMinfin dataset payload: data must be an array");
        }

        Map<String, Integer> columns = columnIndexes(dataSource.columnNames());
        List<CollectedObservationDto> result = new ArrayList<>();
        desiredObservationIndexes = new ArrayList<>();

        for (JsonNode row : dataRows) {
            if (!row.isArray() || row.isEmpty()) {
                continue;
            }

            String caption = textCell(row, columns.getOrDefault("name", 0));
            if (caption == null || caption.isBlank()) {
                continue;
            }

            int startIndex = result.size();

            String canonicalIndicatorCode = caption.trim();
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "plan", ObservationValueKind.PLAN);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "correctConsPlan", ObservationValueKind.REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "correctSubPlan", ObservationValueKind.REFINED_PLAN_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "correctPlanPercent", ObservationValueKind.REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "consFact", ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "subFact", ObservationValueKind.ACTUAL_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "factSubPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "factFOPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT);
            putIfPresent(result, regionCode, canonicalIndicatorCode, year, month, row, columns, "factRFPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION);
            if (indicatorSelector.matches(requestedIndicator, caption)) {
                for (int i = startIndex; i < result.size(); i++) {
                    desiredObservationIndexes.add(i);
                }
            }
        }

        return result;
    }

    private Map<String, Integer> columnIndexes(List<String> columnNames) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            result.put(columnNames.get(i), i);
        }
        return result;
    }

    private void putIfPresent(
            List<CollectedObservationDto> result,
            String regionCode,
            String indicatorCode,
            int year,
            int month,
            JsonNode row,
            Map<String, Integer> columns,
            String columnName,
            ObservationValueKind valueKind
    ) {
        Integer index = columns.get(columnName);
        if (index == null || index >= row.size()) {
            return;
        }
        BigDecimal value = decimalCell(row, index);
        if (value == null) {
            return;
        }

        result.add(new CollectedObservationDto(
                regionCode,
                indicatorCode,
                year,
                month,
                valueKind,
                value,
                true
        ));
    }

    private String textCell(JsonNode row, int index) {
        if (index >= row.size()) {
            return null;
        }
        JsonNode node = row.get(index);
        return node == null || node.isNull() ? null : node.asText();
    }

    private BigDecimal decimalCell(JsonNode row, int index) {
        if (index >= row.size()) {
            return null;
        }
        JsonNode node = row.get(index);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text.replace(" ", "").replace(",", "."));
    }
}
