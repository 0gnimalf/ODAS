package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.application.dto.analysis.MonthlyObservationPointRawDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.Collection;
import java.util.List;

public interface AnalysisQueryPort {

    List<MonthlyObservationPointRawDto> findMonthlyObservationPoints(
            IndicatorGroupCode groupCode,
            Long regionId,
            Long indicatorYearEntryId,
            ObservationValueKind valueKind,
            Collection<Long> periodIds
    );
}
