package Ogni.ODAS.application.port.out.collector;

import Ogni.ODAS.application.dto.ExternalDatasetPayload;
import Ogni.ODAS.application.dto.ExternalRegionRef;

import java.util.Collection;
import java.util.List;

public interface ExternalPopulationCollectorPort {

    List<ExternalDatasetPayload> collectPopulationObservations(
            int year,
            Collection<ExternalRegionRef> regions
    );
}
