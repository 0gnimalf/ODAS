package Ogni.ODAS.application.port.out.analysis;

import Ogni.ODAS.application.dto.analysis.RegionIndicatorMatrixResultDto;
import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public interface RegionIndicatorMatrixPort {

    RegionIndicatorMatrixResultDto calculate(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            ObservationValueKind valueKind,
            UnitCode unitCode,
            List<RegionReadDto> rows,
            List<IndicatorEntryReadDto> columns,
            List<ObservationReadDto> observations
    );
}
