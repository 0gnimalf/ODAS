package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.ObservationRepositoryPort;
import Ogni.ODAS.db.entity.*;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.repository.JpaObservationRepository;
import Ogni.ODAS.db.support.PersistenceReferenceResolver;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ObservationRepositoryAdapter implements ObservationRepositoryPort {

    private final JpaObservationRepository observationRepository;
    private final ObservationEntityMapper mapper;
    private final PersistenceReferenceResolver referenceResolver;

    public ObservationRepositoryAdapter(
            JpaObservationRepository observationRepository,
            ObservationEntityMapper mapper,
            PersistenceReferenceResolver referenceResolver
    ) {
        this.observationRepository = observationRepository;
        this.mapper = mapper;
        this.referenceResolver = referenceResolver;
    }

    @Override
    public List<Observation> findAllByRegionAndIndicatorAndPeriod(
            String regionCode,
            IndicatorGroupCode indicatorGroupCode,
            String indicatorCode,
            Integer year,
            Integer month
    ) {
        return observationRepository
                .findAllByRegionCodeAndIndicatorIndicatorGroupCodeAndIndicatorCodeAndReportingPeriodYearAndReportingPeriodMonth(
                        regionCode, indicatorGroupCode, indicatorCode, year, month
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<Observation> saveAll(List<Observation> observations) {
        if (observations.isEmpty()) {
            return List.of();
        }

        DatasetVersionEntity datasetVersionEntity = referenceResolver.resolveDatasetVersion(observations.getFirst().datasetVersion());

        List<ObservationEntity> entities = observations.stream()
                .map(observation -> {
                    RegionEntity region = referenceResolver.resolveRegion(observation.regionCode());
                    IndicatorEntity indicator = referenceResolver.resolveIndicator(
                            observation.indicatorCode(),
                            observation.indicatorGroupCode()
                    );
                    ReportingPeriodEntity reportingPeriod = referenceResolver.resolveReportingPeriod(observation.reportingPeriod());

                    return observationRepository
                            .findByDatasetVersionIdAndRegionCodeAndIndicatorIndicatorGroupCodeAndIndicatorCodeAndReportingPeriodYearAndReportingPeriodMonthAndValueKind(
                                    datasetVersionEntity.getId(),
                                    observation.regionCode(),
                                    observation.indicatorGroupCode(),
                                    observation.indicatorCode(),
                                    observation.reportingPeriod().year(),
                                    observation.reportingPeriod().month(),
                                    observation.valueKind()
                            )
                            .orElseGet(() -> mapper.toNewEntity(
                                    observation,
                                    datasetVersionEntity,
                                    region,
                                    indicator,
                                    reportingPeriod
                            ));
                })
                .toList();

        return observationRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
