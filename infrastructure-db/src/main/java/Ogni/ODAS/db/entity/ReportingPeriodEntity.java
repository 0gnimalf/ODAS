package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.PeriodType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "reporting_period", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reporting_period_type_year_month_quarter",
                        columnNames = {"periodType", "year", "month", "quarter"}
                )
        },
        indexes = {
                @Index(name = "idx_reporting_period_year_month", columnList = "year, month"),
                @Index(name = "idx_reporting_period_type", columnList = "periodType")
        })
public class ReportingPeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(nullable = false, length = 4)
    private Integer year;

    private Integer month;
    private Integer quarter;

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Column(nullable = false)
    private String label;

}