package Ogni.ODAS.application.port.out.persistence;

import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.Period;

import java.util.Optional;

public interface PeriodPersistencePort {

    Period save(Period period);

    Optional<Period> findByIdentity(PeriodType periodType, Integer year, Integer month, Integer quarter);

    default Period getOrCreateYear(int year) {
        return findByIdentity(PeriodType.YEAR, year, null, null)
                .orElseGet(() -> save(Period.year(year)));
    }

    default Period getOrCreateMonth(int year, int month) {
        return findByIdentity(PeriodType.MONTH, year, month, null)
                .orElseGet(() -> save(Period.month(year, month)));
    }
}
