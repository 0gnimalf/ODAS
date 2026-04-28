package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.IndicatorGroupCode;
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
@Table(name = "indicator",
        uniqueConstraints = @UniqueConstraint(
                name = "idx_indicator_group_name",
                columnNames = {"indicator_group_code", "name"}
        )
)
public class IndicatorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "indicator_seq_gen")
    @SequenceGenerator(name = "indicator_seq_gen", sequenceName = "indicator_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false, length = 2000)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_group_code", nullable = false, length = 32)
    private IndicatorGroupCode indicatorGroupCode;
}
