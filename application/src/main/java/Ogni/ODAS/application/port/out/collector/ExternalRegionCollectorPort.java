package Ogni.ODAS.application.port.out.collector;

import Ogni.ODAS.application.dto.ExternalRegionRow;

import java.util.List;

public interface ExternalRegionCollectorPort {

    List<ExternalRegionRow> collectRegions();
}
