package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "indicator", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_indicator_group_code_parent",
                        columnNames = {"indicatorGroupCode", "code", "parent_id"}
                )
        },
        indexes = {
                @Index(name = "idx_indicator_parent", columnList = "parent_id"),
                @Index(name = "idx_indicator_sort_order", columnList = "sortOrder"),
                @Index(name = "idx_indicator_group_code", columnList = "indicatorGroupCode")
        })
public class IndicatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Lob
    private String code;

    @Column(nullable = false)
    @Lob
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