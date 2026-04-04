package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.CollectedObservationDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.util.IminfinJsonTableHelper;
import Ogni.ODAS.iminfin.util.IminfinTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class IminfinObservationMapper {

    private static final List<ValueBinding> DETAIL_BINDINGS = List.of(
            new ValueBinding("plan", ObservationValueKind.PLAN),
            new ValueBinding("correctConsPlan", ObservationValueKind.REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET),
            new ValueBinding("correctSubPlan", ObservationValueKind.REFINED_PLAN_SUBJECT_BUDGET),
            new ValueBinding("correctPlanPercent", ObservationValueKind.REFINED_PLAN_RATE_TO_PREVIOUS_PERIOD_EXECUTION),
            new ValueBinding("consFact", ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET),
            new ValueBinding("subFact", ObservationValueKind.ACTUAL_SUBJECT_BUDGET),
            new ValueBinding("factSubPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_SUBJECT),
            new ValueBinding("factFOPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_FEDERAL_DISTRICT),
            new ValueBinding("factRFPercent", ObservationValueKind.GROWTH_RATE_TO_PREVIOUS_PERIOD_BY_RUSSIAN_FEDERATION)
    );

    private static final List<ValueBinding> CREDIT_BINDINGS = List.of(
            new ValueBinding("1", ObservationValueKind.PLAN),
            new ValueBinding("2", ObservationValueKind.REFINED_PLAN_CONSOLIDATED_SUBJECT_BUDGET),
            new ValueBinding("3", ObservationValueKind.REFINED_PLAN_SUBJECT_BUDGET),
            new ValueBinding("4", ObservationValueKind.ACTUAL_CONSOLIDATED_SUBJECT_BUDGET),
            new ValueBinding("5", ObservationValueKind.ACTUAL_SUBJECT_BUDGET)
    );

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
        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(dataSource.columnNames());

        for (IminfinParsedIndicatorRow parsed : indicatorTreeParser.parseDetailRows(rootPrefix, dataSource, dataRows)) {
            addObservations(result, regionCode, indicatorGroupCode, parsed.code(), year, month, parsed.row(), columns, DETAIL_BINDINGS);
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

        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(dataSource.columnNames());
        Integer nameIndex = columns.getOrDefault("name", 0);
        List<CollectedObservationDto> result = new ArrayList<>();

        for (JsonNode row : dataRows) {
            if (!row.isArray() || row.isEmpty()) {
                continue;
            }

            String caption = IminfinJsonTableHelper.textCell(row, nameIndex);
            String regionCode = resolveRegionCode(caption, regionCodeByNormalizedName);
            if (regionCode == null) {
                continue;
            }

            addObservations(result, regionCode, IndicatorGroupCode.CREDIT, requestedIndicatorCode, year, month, row, columns, CREDIT_BINDINGS);
        }
        return result;
    }

    private void addObservations(
            List<CollectedObservationDto> result,
            String regionCode,
            IndicatorGroupCode indicatorGroupCode,
            String indicatorCode,
            int year,
            int month,
            JsonNode row,
            Map<String, Integer> columns,
            List<ValueBinding> bindings
    ) {
        for (ValueBinding binding : bindings) {
            putIfPresent(result, regionCode, indicatorGroupCode, indicatorCode, year, month, row, columns, binding);
        }
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
            ValueBinding binding
    ) {
        Integer index = resolveColumnIndex(columns, binding.columnName());
        BigDecimal value = IminfinJsonTableHelper.decimalCell(row, index);
        if (value == null) {
            return;
        }

        result.add(new CollectedObservationDto(
                regionCode,
                indicatorGroupCode,
                indicatorCode,
                year,
                month,
                binding.valueKind(),
                value,
                true
        ));
    }

    private Integer resolveColumnIndex(Map<String, Integer> columns, String columnName) {
        if (columns == null || columns.isEmpty()) {
            return parseNumericColumn(columnName);
        }

        Integer index = columns.get(columnName);
        return index != null ? index : parseNumericColumn(columnName);
    }

    private Integer parseNumericColumn(String columnName) {
        try {
            return Integer.valueOf(columnName);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveRegionCode(String rowCaption, Map<String, String> regionCodeByNormalizedName) {
        if (regionCodeByNormalizedName == null || regionCodeByNormalizedName.isEmpty()) {
            return null;
        }
        return regionCodeByNormalizedName.get(IminfinTextNormalizer.normalize(rowCaption));
    }

    private record ValueBinding(String columnName, ObservationValueKind valueKind) {
    }
}
