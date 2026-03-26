package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.ObservationValueKind;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "observation", schema = "a")
public class ObservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private DatasetVersionEntity datasetVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private IndicatorEntity indicator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ReportingPeriodEntity reportingPeriod;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ObservationValueKind valueKind;

    @Column(nullable = false)
    private BigDecimal value;
}