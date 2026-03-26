package Ogni.ODAS.db.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "population_stat", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_population_stat_region_period",
                        columnNames = {"region_id", "reporting_period_id"}
                )
        },
        indexes = {
                @Index(name = "idx_population_stat_region", columnList = "region_id"),
                @Index(name = "idx_population_stat_period", columnList = "reporting_period_id")
        })
public class PopulationStatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ReportingPeriodEntity reportingPeriod;

    private Long populationValue;
}