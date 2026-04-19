package Ogni.ODAS.application.port.out.collector;

import Ogni.ODAS.application.dto.ExternalDatasetPayload;
import Ogni.ODAS.application.dto.ExternalRegionRef;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;

import java.util.Collection;
import java.util.List;

public interface ExternalObservationCollectorPort {

    List<ExternalDatasetPayload> collectObservations(
            IndicatorGroupCode groupCode,
            int year,
            int month,
            Collection<ExternalRegionRef> regions
    );
}
