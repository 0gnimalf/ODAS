package Ogni.ODAS.db.adapter;

import Ogni.ODAS.application.dto.analysis.MonthlyObservationPointRawDto;
import Ogni.ODAS.application.port.out.persistence.AnalysisQueryPort;
import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AnalysisQueryAdapter implements AnalysisQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AnalysisQueryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public List<MonthlyObservationPointRawDto> findMonthlyObservationPoints(
            IndicatorGroupCode groupCode,
            Long regionId,
            Long indicatorYearEntryId,
            ObservationValueKind valueKind,
            Collection<Long> periodIds
    ) {
        List<Long> normalizedPeriodIds = periodIds == null ? List.of() : periodIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedPeriodIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT
                    p.id AS period_id,
                    p.year,
                    p.month,
                    p.label,
                    o.value
                FROM observation o
                    JOIN period p ON p.id = o.period_id
                    JOIN indicator_year_entry iye ON iye.id = o.indicator_year_entry_id
                    JOIN indicator i ON i.id = iye.indicator_id
                WHERE i.indicator_group_code = :groupCode
                  AND o.region_id = :regionId
                  AND o.indicator_year_entry_id = :indicatorYearEntryId
                  AND o.observation_value_kind = :valueKind
                  AND o.period_id IN (:periodIds)
                ORDER BY p.year, p.month
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupCode", groupCode.name())
                .addValue("regionId", regionId)
                .addValue("indicatorYearEntryId", indicatorYearEntryId)
                .addValue("valueKind", valueKind.name())
                .addValue("periodIds", normalizedPeriodIds);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> toMonthlyPoint(rs));
    }

    private MonthlyObservationPointRawDto toMonthlyPoint(ResultSet rs) throws SQLException {
        return new MonthlyObservationPointRawDto(
                rs.getLong("period_id"),
                rs.getInt("year"),
                rs.getInt("month"),
                rs.getString("label"),
                rs.getBigDecimal("value")
        );
    }
}
