package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.ObservationRepositoryPort;
import Ogni.ODAS.db.entity.DatasetVersionEntity;
import Ogni.ODAS.db.mapper.DatasetVersionEntityMapper;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.repository.JpaDatasetVersionRepository;
import Ogni.ODAS.db.repository.JpaObservationRepository;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObservationRepositoryAdapter implements ObservationRepositoryPort {

    private final JpaObservationRepository observationRepository;
    private final JpaDatasetVersionRepository datasetVersionRepository;
    private final ObservationEntityMapper mapper;

    public ObservationRepositoryAdapter(
            JpaObservationRepository observationRepository,
            JpaDatasetVersionRepository datasetVersionRepository,
            ObservationEntityMapper mapper
    ) {
        this.observationRepository = observationRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Observation> findAllByRegionIndicatorAndPeriod(
            String regionCode,
            String indicatorCode,
            Integer year,
            Integer month
    ) {
        return observationRepository
                .findAllByRegion_CodeAndIndicator_CodeAndReportingPeriod_YearAndReportingPeriod_Month(
                        regionCode, indicatorCode, year, month
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Observation> saveAll(List<Observation> observations) {
        if (observations.isEmpty()) {
            return List.of();
        }

        DatasetVersionEntity datasetVersionEntity = datasetVersionRepository.save(
                DatasetVersionEntityMapper.toEntity(observations.getFirst().datasetVersion())
        );

        var entities = observations.stream()
                .map(obs -> mapper.toEntity(obs, datasetVersionEntity))
                .toList();

        return observationRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
