package Ogni.ODAS.application.port.out;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Indicator;
import Ogni.ODAS.domain.model.IndicatorYearEntry;

import java.util.List;
import java.util.Optional;

public interface IndicatorRepositoryPort {

    Indicator save(Indicator indicator);

    Optional<Indicator> findByCodeAndGroupCode(String code, IndicatorGroupCode groupCode);

    void replaceYearEntries(IndicatorGroupCode groupCode, Integer year, List<IndicatorYearEntry> entries);

    List<IndicatorYearEntry> findAllByGroupCodeAndYear(IndicatorGroupCode groupCode, Integer year);
}
