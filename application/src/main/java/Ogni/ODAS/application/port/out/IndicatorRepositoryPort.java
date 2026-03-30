package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;

import java.util.List;
import java.util.Optional;

public interface IndicatorRepositoryPort {

    Indicator save(Indicator indicator);

    Optional<Indicator> findByCodeAndGroupCode(String code, IndicatorGroupCode groupCode);

    List<Indicator> findAllByGroupCode(IndicatorGroupCode groupCode);
}
