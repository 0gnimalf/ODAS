package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;

import java.util.List;
import java.util.Optional;

public interface IndicatorPersistencePort {

    Indicator save(Indicator indicator);

    Optional<Indicator> findByNameAndGroup(String name, IndicatorGroupCode groupCode);

    List<Indicator> findAllByGroup(IndicatorGroupCode groupCode);
}
