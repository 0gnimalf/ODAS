package Ogni.ODAS.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "indicator_year_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_indicator_year_entry_indicator_id_period_id",
                columnNames = {"indicator_id", "period_id"}
        ),
        indexes = {
                @Index(name = "idx_indicator_year_entry_period_sort", columnList = "period_id, sort_order"),
                @Index(name = "idx_indicator_year_entry_parent_sort", columnList = "parent_indicator_year_entry_id, sort_order")
        }
)
public class IndicatorYearEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "indicator_year_entry_seq_gen")
    @SequenceGenerator(name = "indicator_year_entry_seq_gen", sequenceName = "indicator_year_entry_seq", allocationSize = 1)
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "parent_indicator_year_entry_id")
    private Long parentIndicatorYearEntryId;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "has_children", nullable = false)
    private boolean hasChildren;
}
