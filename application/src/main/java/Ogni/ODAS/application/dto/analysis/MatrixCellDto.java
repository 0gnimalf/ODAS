package Ogni.ODAS.application.dto.analysis;

import java.math.BigDecimal;

public record MatrixCellDto(
        Long regionId,
        Long indicatorYearEntryId,
        BigDecimal value,
        boolean missing
) {
}
