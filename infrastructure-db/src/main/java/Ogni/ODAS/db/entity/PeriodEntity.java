package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.PeriodType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "period",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_period_period_type_year_month_quarter",
                columnNames = {"period_type", "year", "month", "quarter"}
        )
)
public class PeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "period_seq_gen")
    @SequenceGenerator(name = "period_seq_gen", sequenceName = "period_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 16)
    private PeriodType periodType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "quarter")
    private Integer quarter;

    @Column(name = "label", nullable = false, length = 100)
    private String label;
}
