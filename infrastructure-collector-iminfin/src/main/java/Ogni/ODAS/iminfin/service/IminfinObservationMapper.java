package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IminfinObservationMapper {

    private final IminfinIndicatorTreeParser indicatorTreeParser;

    public IminfinObservationMapper(IminfinIndicatorTreeParser indicatorTreeParser) {
        this.indicatorTreeParser = indicatorTreeParser;
    }

    public List<CollectedObservationDto> mapDetailObservationsForRegion(
            String regionCode,
            IndicatorGroupCode indicatorGroupCode,
            int year,
            int month,
            String rootPrefix,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows
    ) {
        List<CollectedObservationDto> result = new ArrayList<>();
        Map<String, Integer> columns = columnIndexes(dataSource.columnNames());

        for (IminfinParsedIndicatorRow parsed : indicatorTreeParser.parseDetailRows(rootPrefix, dataSource, dataRows)) {
            JsonNode row = parsed.row();
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "plan", ObservationValueKind.PLAN);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "correctConsPlan", ObservationValueKind.REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "correctSubPlan", ObservationValueKind.REFINED_PLAN_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "correctPlanPercent", ObservationValueKind.REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "consFact", ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "subFact", ObservationValueKind.ACTUAL_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "factSubPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "factFOPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT);
            putIfPresent(result, regionCode, indicatorGroupCode, parsed.code(), year, month, row, columns, "factRFPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION);
        }

        return result;
    }

    public List<CollectedObservationDto> mapCreditObservationsForIndicator(
            String requestedIndicatorCode,
            int year,
            int month,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows,
            Map<String, String> regionCodeByNormalizedName
    ) {
        if (!dataRows.isArray()) {
            throw new IllegalStateException("Unexpected iMinfin credit dataset payload: data must be an array");
        }

        Map<String, Integer> columns = columnIndexes(dataSource.columnNames());
        Integer nameIndex = columns.getOrDefault("name", 0);
        List<CollectedObservationDto> result = new ArrayList<>();

        for (JsonNode row : dataRows) {
            if (!row.isArray() || row.isEmpty()) {
                continue;
            }
            String caption = textCell(row, nameIndex);
            String regionCode = resolveRegionCode(caption, regionCodeByNormalizedName);
            if (regionCode == null) {
                continue;
            }
            putIfPresent(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, "1", ObservationValueKind.PLAN);
            putIfPresent(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, "2", ObservationValueKind.REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, "3", ObservationValueKind.REFINED_PLAN_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, "4", ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET);
            putIfPresent(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, "5", ObservationValueKind.ACTUAL_SUBJECT_BUDGET);
        }
        return result;
    }

    private String resolveRegionCode(String rowCaption, Map<String, String> regionCodeByNormalizedName) {
        if (regionCodeByNormalizedName == null || regionCodeByNormalizedName.isEmpty()) {
            return null;
        }
        return regionCodeByNormalizedName.get(IminfinTextNormalizer.normalize(rowCaption));
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
            IndicatorGroupCode indicatorGroupCode,
            String indicatorCode,
            int year,
            int month,
            JsonNode row,
            Map<String, Integer> columns,
            String columnName,
            ObservationValueKind valueKind
    ) {
        Integer index;
        if (columns == null || columns.isEmpty()) {
            index = Integer.parseInt(columnName);
        } else {
            index = columns.get(columnName);
        }
        if (index == null || index >= row.size()) {
            return;
        }
        BigDecimal value = decimalCell(row, index);
        if (value == null) {
            return;
        }

        result.add(new CollectedObservationDto(
                regionCode,
                indicatorGroupCode,
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
