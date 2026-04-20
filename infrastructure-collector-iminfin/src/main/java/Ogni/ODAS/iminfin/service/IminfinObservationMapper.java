package Ogni.ODAS.iminfin.service;

import Ogni.ODAS.application.dto.ExternalObservationRow;
import Ogni.ODAS.application.support.TextNormalizer;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import Ogni.ODAS.iminfin.model.IminfinDataSourceDefinition;
import Ogni.ODAS.iminfin.model.IminfinParsedIndicatorRow;
import Ogni.ODAS.iminfin.util.IminfinJsonTableHelper;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public List<ExternalObservationRow> mapDetailObservations(
            String regionExternalCode,
            IndicatorGroupCode groupCode,
            IminfinDataSourceDefinition dataSource,
            List<IminfinParsedIndicatorRow> parsedRows
    ) {
        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(dataSource.columnNames());
        List<ExternalObservationRow> result = new ArrayList<>();
        for (IminfinParsedIndicatorRow row : parsedRows) {
            addObservations(result, regionExternalCode, groupCode, row.name(), row.parentName(), row.row(), columns, DETAIL_BINDINGS);
        }
        return result;
    }

    public List<ExternalObservationRow> mapCreditObservations(
            String indicatorName,
            String parentIndicatorName,
            IminfinDataSourceDefinition dataSource,
            JsonNode dataRows,
            Map<String, String> externalRegionCodeByNormalizedName
    ) {
        if (!dataRows.isArray()) {
            throw new IllegalStateException("Unexpected iMinfin credit dataset payload: data must be an array");
        }
        Map<String, Integer> columns = IminfinJsonTableHelper.columnIndexes(dataSource.columnNames());
        Integer nameIndex = columns.getOrDefault("name", 0);
        List<ExternalObservationRow> result = new ArrayList<>();
        for (JsonNode row : dataRows) {
            String caption = IminfinJsonTableHelper.textCell(row, nameIndex);
            String regionExternalCode = externalRegionCodeByNormalizedName.get(TextNormalizer.normalize(caption));
            if (regionExternalCode == null) {
                continue;
            }
            addObservations(result, regionExternalCode, IndicatorGroupCode.CREDIT, indicatorName, parentIndicatorName, row, columns, CREDIT_BINDINGS);
        }
        return result;
    }

    private void addObservations(
            List<ExternalObservationRow> result,
            String regionExternalCode,
            IndicatorGroupCode groupCode,
            String indicatorName,
            String parentIndicatorName,
            JsonNode row,
            Map<String, Integer> columns,
            List<ValueBinding> bindings
    ) {
        for (ValueBinding binding : bindings) {
            Integer index = resolveColumnIndex(columns, binding.columnName());
            BigDecimal value = IminfinJsonTableHelper.decimalCell(row, index);
            if (value == null) {
                continue;
            }
            result.add(new ExternalObservationRow(
                    SourceSystemCode.IMINFIN,
                    regionExternalCode,
                    groupCode,
                    indicatorName,
                    parentIndicatorName,
                    binding.valueKind(),
                    value
            ));
        }
    }

    private Integer resolveColumnIndex(Map<String, Integer> columns, String columnName) {
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

    private record ValueBinding(String columnName, ObservationValueKind valueKind) {
    }
}
