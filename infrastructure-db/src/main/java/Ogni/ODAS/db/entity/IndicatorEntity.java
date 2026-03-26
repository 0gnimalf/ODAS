package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "indicator", schema = "a")
public class IndicatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IndicatorGroupCode indicatorGroupCode;

    @ManyToOne(fetch = FetchType.LAZY)
    private IndicatorEntity parent;

    private Integer level;

    private Integer sortOrder;
}