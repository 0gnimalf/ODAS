package Ogni.ODAS.db.iminfin.adapter;

import Ogni.ODAS.application.iminfin.port.out.ReportingPeriodDictionaryRepositoryPort;
import Ogni.ODAS.db.entity.ReportingPeriodEntity;
import Ogni.ODAS.db.mapper.ReportingPeriodEntityMapper;
import Ogni.ODAS.db.repository.JpaReportingPeriodRepository;
import Ogni.ODAS.domain.enumtype.PeriodType;
import Ogni.ODAS.domain.model.ReportingPeriod;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ReportingPeriodDictionaryRepositoryAdapter implements ReportingPeriodDictionaryRepositoryPort {

    private final JpaReportingPeriodRepository repository;

    public ReportingPeriodDictionaryRepositoryAdapter(JpaReportingPeriodRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ReportingPeriod> findMonthly(Integer year, Integer month) {
        return repository.findByPeriodTypeAndYearAndMonthAndQuarter(PeriodType.MONTH, year, month, null)
                .map(ReportingPeriodEntityMapper::toDomain);
    }

    @Override
    public ReportingPeriod save(ReportingPeriod reportingPeriod) {
        ReportingPeriodEntity entity = new ReportingPeriodEntity();
        entity.setId(reportingPeriod.id());
        entity.setPeriodType(reportingPeriod.type());
        entity.setYear(reportingPeriod.year());
        entity.setMonth(reportingPeriod.month());
        entity.setQuarter(reportingPeriod.quarter());
        entity.setDateFrom(reportingPeriod.dateFrom());
        entity.setDateTo(reportingPeriod.dateTo());
        entity.setLabel(reportingPeriod.label());
        return ReportingPeriodEntityMapper.toDomain(repository.save(entity));
    }
}
