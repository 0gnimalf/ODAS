package Ogni.ODAS.application.port.out.analysis;

import Ogni.ODAS.application.dto.analysis.MonthlySeriesPointDto;
import Ogni.ODAS.application.dto.analysis.QuarterAggregateDto;

import java.util.List;

public interface QuarterAggregationPort {

    List<QuarterAggregateDto> aggregate(List<MonthlySeriesPointDto> points);
}
