package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.dto.read.IndicatorEntryReadDto;
import Ogni.ODAS.application.dto.read.ObservationReadDto;
import Ogni.ODAS.application.dto.read.RegionReadDto;
import Ogni.ODAS.application.port.out.persistence.StoredDataQueryPort;
import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class StoredDataQueryAdapter implements StoredDataQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StoredDataQueryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public List<RegionReadDto> findRegions() {
        String sql = """
                SELECT id, name, federal_district_code
                FROM region
                ORDER BY name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toRegion(rs));
    }

    @Override
    public List<IndicatorEntryReadDto> findIndicatorEntries(IndicatorGroupCode groupCode, Long yearPeriodId) {
        String sql = """
                SELECT
                    iye.id,
                    iye.period_id,
                    iye.indicator_id,
                    i.name,
                    i.indicator_group_code,
                    iye.parent_indicator_year_entry_id,
                    iye.level,
                    iye.sort_order,
                    iye.has_children
                FROM indicator_year_entry iye
                    JOIN indicator i ON i.id = iye.indicator_id
                WHERE iye.period_id = :periodId
                  AND i.indicator_group_code = :groupCode
                ORDER BY iye.level, iye.sort_order
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("periodId", yearPeriodId)
                .addValue("groupCode", groupCode.name());
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> toIndicatorEntry(rs));
    }

    @Override
    public List<ObservationReadDto> findObservations(
            IndicatorGroupCode groupCode,
            Long periodId,
            Collection<Long> regionIds,
            Collection<Long> indicatorYearEntryIds,
            Set<ObservationValueKind> valueKinds
    ) {
        List<Long> normalizedRegionIds = requireNonEmptyLongFilter(regionIds, "regionIds");
        List<Long> normalizedIndicatorYearEntryIds = requireNonEmptyLongFilter(indicatorYearEntryIds, "indicatorYearEntryIds");

        StringBuilder sql = new StringBuilder("""
                SELECT
                    o.id AS observation_id,
                    o.region_id,
                    r.name AS region_name,
                    o.indicator_year_entry_id,
                    i.name AS indicator_name,
                    o.observation_value_kind,
                    o.value,
                    o.dataset_collection_id
                FROM observation o
                    JOIN region r ON r.id = o.region_id
                    JOIN indicator_year_entry iye ON iye.id = o.indicator_year_entry_id
                    JOIN indicator i ON i.id = iye.indicator_id
                WHERE o.period_id = :periodId
                  AND i.indicator_group_code = :groupCode
                  AND o.region_id IN (:regionIds)
                  AND o.indicator_year_entry_id IN (:indicatorYearEntryIds)
                """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("periodId", periodId)
                .addValue("groupCode", groupCode.name())
                .addValue("regionIds", normalizedRegionIds)
                .addValue("indicatorYearEntryIds", normalizedIndicatorYearEntryIds);

        if (valueKinds != null && !valueKinds.isEmpty()) {
            params.addValue("valueKinds", valueKinds.stream().map(Enum::name).toList());
            sql.append(" AND o.observation_value_kind IN (:valueKinds)\n");
        }
//        sql.append(" ORDER BY r.name, iye.sort_order, i.name");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> toObservation(rs));
    }

    private List<Long> requireNonEmptyLongFilter(Collection<Long> values, String parameterName) {
        List<Long> normalized = values == null
                ? List.of()
                : values.stream().filter(Objects::nonNull).distinct().toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " must contain at least one id");
        }
        return normalized;
    }

    private RegionReadDto toRegion(ResultSet rs) throws SQLException {
        FederalDistrictCode federalDistrictCode = FederalDistrictCode.valueOf(rs.getString("federal_district_code"));
        return new RegionReadDto(
                rs.getLong("id"),
                rs.getString("name"),
                federalDistrictCode,
                federalDistrictCode.getName(),
                federalDistrictCode.getFullName(),
                federalDistrictCode.getShortName()
        );
    }

    private IndicatorEntryReadDto toIndicatorEntry(ResultSet rs) throws SQLException {
        return new IndicatorEntryReadDto(
                rs.getLong("id"),
                rs.getLong("period_id"),
                rs.getLong("indicator_id"),
                rs.getString("name"),
                IndicatorGroupCode.valueOf(rs.getString("indicator_group_code")),
                nullableLong(rs, "parent_indicator_year_entry_id"),
                rs.getInt("level"),
                rs.getInt("sort_order"),
                rs.getBoolean("has_children")
        );
    }

    private ObservationReadDto toObservation(ResultSet rs) throws SQLException {
        ObservationValueKind valueKind = ObservationValueKind.valueOf(rs.getString("observation_value_kind"));
        return new ObservationReadDto(
                rs.getLong("observation_id"),
                rs.getLong("region_id"),
                rs.getString("region_name"),
                rs.getLong("indicator_year_entry_id"),
                rs.getString("indicator_name"),
                valueKind,
                valueKind.getLabel(),
                valueKind.getUnitCode(),
                valueKind.getUnitCode().getLabel(),
                valueKind.getObservationValueType(),
                rs.getBigDecimal("value"),
                rs.getLong("dataset_collection_id")
        );
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
