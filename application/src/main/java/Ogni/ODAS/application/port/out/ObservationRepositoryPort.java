package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.model.Observation;

import java.util.List;


public interface ObservationRepositoryPort {

    List<Observation> findAllByRegionIndicatorAndPeriod(
            String regionCode,
            String indicatorCode,
            Integer year,
            Integer month
    );

    List<Observation> saveAll(List<Observation> observations);
}
