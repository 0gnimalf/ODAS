package Ogni.ODAS.application.port.out.analysis;

import Ogni.ODAS.application.dto.analysis.RegionComparisonResultDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.enumtype.UnitCode;

import java.util.List;

public interface RegionComparisonPort {

    RegionComparisonResultDto calculate(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            Long indicatorYearEntryId,
            String indicatorName,
            ObservationValueKind valueKind,
            UnitCode unitCode,
            List<RegionReadDto> requestedRegions,
            List<ObservationReadDto> observations
    );
}
