package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "observation", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_observation_dataset_region_indicator_period_kind",
                        columnNames = {
                                "dataset_version_id",
                                "region_id",
                                "indicator_id",
                                "reporting_period_id",
                                "value_kind"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_observation_region_indicator_period",
                        columnList = "region_id, indicator_id, reporting_period_id"
                ),
                @Index(
                        name = "idx_observation_dataset_version",
                        columnList = "dataset_version_id"
                ),
                @Index(
                        name = "idx_observation_value_kind",
                        columnList = "value_kind"
                )
        })
public class ObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private DatasetVersionEntity datasetVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private IndicatorEntity indicator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ObservationValueKind valueKind;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "is_cumulative", nullable = false)
    private boolean cumulative;
}