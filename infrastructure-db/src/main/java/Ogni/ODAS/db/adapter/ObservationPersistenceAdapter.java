package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.ObservationPersistencePort;
import Ogni.ODAS.db.entity.ObservationEntity;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.repository.ObservationJpaRepository;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;

import java.util.Optional;

public class ObservationPersistenceAdapter implements ObservationPersistencePort {

    private final ObservationJpaRepository repository;

    public ObservationPersistenceAdapter(ObservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Observation save(Observation observation) {
        return ObservationEntityMapper.toDomain(repository.save(ObservationEntityMapper.toEntity(observation)));
    }

    @Override
    public Observation upsertCurrent(Observation observation) {
        Optional<ObservationEntity> existing = repository.findByRegionIdAndIndicatorYearEntryIdAndPeriodIdAndObservationValueKind(
                observation.regionId(),
                observation.indicatorYearEntryId(),
                observation.periodId(),
                observation.observationValueKind()
        );
        if (existing.isEmpty()) {
            return save(observation);
        }
        ObservationEntity entity = existing.get();
        entity.setDatasetCollectionId(observation.datasetCollectionId());
        entity.setValue(observation.value());
        return ObservationEntityMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Observation> findCurrent(Long regionId, Long indicatorYearEntryId, Long periodId, ObservationValueKind observationValueKind) {
        return repository.findByRegionIdAndIndicatorYearEntryIdAndPeriodIdAndObservationValueKind(
                        regionId,
                        indicatorYearEntryId,
                        periodId,
                        observationValueKind
                )
                .map(ObservationEntityMapper::toDomain);
    }
}
