package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_observation_region_id_indicator_year_entry_id_period_id_observation_value_kind",
                columnNames = {"region_id", "indicator_year_entry_id", "period_id", "observation_value_kind"}
        ),
        indexes = {
                @Index(name = "idx_observation_dataset_collection_id", columnList = "dataset_collection_id"),
                @Index(name = "idx_observation_region_id_period_id", columnList = "region_id, period_id"),
                @Index(name = "idx_observation_indicator_year_entry_id_period_id", columnList = "indicator_year_entry_id, period_id"),
                @Index(name = "idx_observation_period_id", columnList = "period_id")
        }
)
public class ObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "observation_seq_gen")
    @SequenceGenerator(name = "observation_seq_gen", sequenceName = "observation_seq", allocationSize = 1)
    private Long id;

    @Column(name = "dataset_collection_id", nullable = false)
    private Long datasetCollectionId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "indicator_year_entry_id", nullable = false)
    private Long indicatorYearEntryId;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_value_kind", nullable = false, length = 128)
    private ObservationValueKind observationValueKind;

    @Column(name = "value", nullable = false, precision = 24, scale = 8)
    private BigDecimal value;
}
