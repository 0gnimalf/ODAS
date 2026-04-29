package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.port.out.persistence.ObservationPersistencePort;
import Ogni.ODAS.db.mapper.ObservationEntityMapper;
import Ogni.ODAS.db.repository.ObservationJpaRepository;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import Ogni.ODAS.domain.model.Observation;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

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
            ON CONFLICT (period_id, region_id, indicator_year_entry_id, observation_value_kind)
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

    private static List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
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

    @Override
    public Set<Long> findRegionIdsWithCompleteCurrentObservations(
            Collection<Long> regionIds,
            Long indicatorYearEntryId,
            Collection<Long> periodIds,
            ObservationValueKind valueKind
    ) {
        List<Long> normalizedRegionIds = normalizeIds(regionIds);
        List<Long> normalizedPeriodIds = normalizeIds(periodIds);
        if (normalizedRegionIds.isEmpty()
                || normalizedPeriodIds.isEmpty()
                || indicatorYearEntryId == null
                || valueKind == null) {
            return Set.of();
        }

        String regionPlaceholders = placeholders(normalizedRegionIds.size());
        String periodPlaceholders = placeholders(normalizedPeriodIds.size());
        String sql = """
                SELECT region_id
                FROM observation
                WHERE region_id IN (%s)
                  AND indicator_year_entry_id = ?
                  AND period_id IN (%s)
                  AND observation_value_kind = ?
                GROUP BY region_id
                HAVING COUNT(DISTINCT period_id) = ?
                """.formatted(regionPlaceholders, periodPlaceholders);

        List<Object> args = new ArrayList<>(normalizedRegionIds.size() + normalizedPeriodIds.size() + 3);
        args.addAll(normalizedRegionIds);
        args.add(indicatorYearEntryId);
        args.addAll(normalizedPeriodIds);
        args.add(valueKind.name());
        args.add(normalizedPeriodIds.size());

        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("region_id"), args.toArray()));
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
