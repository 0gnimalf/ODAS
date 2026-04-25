package Ogni.ODAS.application.dto.analysis;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public record RegionIndicatorMatrixResultDto(
        IndicatorGroupCode groupCode,
        Integer year,
        Integer month,
        ObservationValueKind valueKind,
        String valueKindLabel,
        UnitCode unitCode,
        String unitCodeLabel,
        List<RegionIndicatorMatrixRowDto> rows,
        List<RegionIndicatorMatrixColumnDto> columns,
        List<MatrixCellDto> cells
) {
}
