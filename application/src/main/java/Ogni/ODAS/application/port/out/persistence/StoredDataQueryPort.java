package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface StoredDataQueryPort {

    List<RegionReadDto> findRegions();

    List<IndicatorEntryReadDto> findIndicatorEntries(IndicatorGroupCode groupCode, Long yearPeriodId);

    List<ObservationReadDto> findObservations(
            IndicatorGroupCode groupCode,
            Long periodId,
            Collection<Long> regionIds,
            Collection<Long> indicatorYearEntryIds,
            Set<ObservationValueKind> valueKinds
    );
}
