package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "indicator", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_indicator_group_code_code",
                        columnNames = {"indicatorGroupCode", "code"}
                )
        },
        indexes = {
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
    @Enumerated(EnumType.STRING)
    private IndicatorGroupCode indicatorGroupCode;
}
