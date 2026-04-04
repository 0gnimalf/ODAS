package Ogni.ODAS.db.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "indicator_year_entry", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_indicator_year_entry_indicator_year",
                        columnNames = {"indicator_id", "year_value"}
                )
        },
        indexes = {
                @Index(name = "idx_indicator_year_entry_year", columnList = "year_value"),
                @Index(name = "idx_indicator_year_entry_parent_indicator", columnList = "parent_indicator_id"),
                @Index(name = "idx_indicator_year_entry_sort_order", columnList = "sortOrder")
        })
public class IndicatorYearEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "indicator_id", nullable = false)
    private IndicatorEntity indicator;

    @Column(nullable = false)
    private Integer yearValue;

    @Column(nullable = false)
    @Lob
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_indicator_id")
    private IndicatorEntity parentIndicator;

    private Integer level;

    private Integer sortOrder;

    @Column(name = "is_section")
    private boolean section;
}
