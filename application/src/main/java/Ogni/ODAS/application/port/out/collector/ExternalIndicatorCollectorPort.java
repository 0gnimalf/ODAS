package Ogni.ODAS.application.port.out.collector;

import Ogni.ODAS.application.dto.ExternalIndicatorRow;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.List;

public interface ExternalIndicatorCollectorPort {

    List<ExternalIndicatorRow> collectIndicators(IndicatorGroupCode groupCode, int year);
}
