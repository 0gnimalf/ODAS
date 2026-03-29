package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "indicator", schema = "a",
        indexes = {
                @Index(name = "idx_indicator_parent", columnList = "parent_id"),
                @Index(name = "idx_indicator_sort_order", columnList = "sortOrder")
        })
public class IndicatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1000)
    private String code;

    @Column(nullable = false, length = 1000)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IndicatorGroupCode indicatorGroupCode;

    @ManyToOne(fetch = FetchType.LAZY)
    private IndicatorEntity parent;

    private Integer level;

    private Integer sortOrder;

    @Column(name = "is_section")
    private boolean section;
}