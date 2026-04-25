package Ogni.ODAS.application.port.out.analysis;

import Ogni.ODAS.application.dto.analysis.MonthlyObservationPointRawDto;
import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.NonCumulativeValueMode;

import java.util.List;

public interface NonCumulativeValuePort {

    List<MonthlySeriesPointDto> calculate(
            List<MonthlyObservationPointRawDto> rawPoints,
            boolean absoluteValue,
            NonCumulativeValueMode mode
    );
}
