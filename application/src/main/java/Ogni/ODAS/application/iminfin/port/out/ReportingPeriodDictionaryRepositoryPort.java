package Ogni.ODAS.application.iminfin.port.out;

import Ogni.ODAS.domain.model.ReportingPeriod;

import java.util.Optional;

public interface ReportingPeriodDictionaryRepositoryPort {

    Optional<ReportingPeriod> findMonthly(Integer year, Integer month);

    ReportingPeriod save(ReportingPeriod reportingPeriod);
}
