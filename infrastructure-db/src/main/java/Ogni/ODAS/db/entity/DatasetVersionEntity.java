package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "dataset_version", schema = "a")
public class DatasetVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String versionLabel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SourceSystemCode sourceSystem;

    @Column(nullable = false)
    private OffsetDateTime collectedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;
}