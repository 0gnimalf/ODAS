package Ogni.ODAS.application.dto.analysis;

public record RegionIndicatorMatrixColumnDto(
        Long indicatorYearEntryId,
        Long indicatorId,
        String indicatorName,
        Integer level,
        Integer sortOrder
) {
}
