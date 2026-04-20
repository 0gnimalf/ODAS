package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.ObservationPersistencePort;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.repository.ObservationJpaRepository;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ObservationPersistenceAdapter implements ObservationPersistencePort {

    private static final String UPSERT_SQL = """
            INSERT INTO observation (
                dataset_collection_id,
                region_id,
                indicator_year_entry_id,
                period_id,
                observation_value_kind,
                value
            )
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (region_id, indicator_year_entry_id, period_id, observation_value_kind)
            DO UPDATE SET
                dataset_collection_id = EXCLUDED.dataset_collection_id,
                value = EXCLUDED.value
            """;

    private final ObservationJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public ObservationPersistenceAdapter(ObservationJpaRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Observation save(Observation observation) {
        return ObservationEntityMapper.toDomain(repository.save(ObservationEntityMapper.toEntity(observation)));
    }

    @Override
    public Observation upsertCurrent(Observation observation) {
        upsertCurrentBatch(List.of(observation));
        return findCurrent(
                observation.regionId(),
                observation.indicatorYearEntryId(),
                observation.periodId(),
                observation.observationValueKind()
        ).orElse(observation);
    }

    @Override
    public int upsertCurrentBatch(Collection<Observation> observations) {
        if (observations == null || observations.isEmpty()) {
            return 0;
        }
        List<Observation> batch = List.copyOf(observations);
        int[][] affected = jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                batch,
                500,
                this::bindObservation
        );
        int total = 0;
        for (int[] batchResult : affected) {
            for (int count : batchResult) {
                if (count > 0) {
                    total += count;
                }
            }
        }
        System.out.println(batch.size() + "|||||" + total);
        return total;
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

    private void bindObservation(PreparedStatement ps, Observation observation) throws SQLException {
        ps.setLong(1, observation.datasetCollectionId());
        ps.setLong(2, observation.regionId());
        ps.setLong(3, observation.indicatorYearEntryId());
        ps.setLong(4, observation.periodId());
        ps.setString(5, observation.observationValueKind().name());
        ps.setBigDecimal(6, observation.value());
    }
}
