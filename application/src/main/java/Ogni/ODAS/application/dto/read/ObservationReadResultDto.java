package Ogni.ODAS.application.dto.read;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.List;

public record ObservationReadResultDto(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        Long periodId,
        List<Long> regionIds,
        List<Long> indicatorYearEntryIds,
        int total,
        List<ObservationReadDto> observations
) {
}
