package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dataset_version", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dataset_version_code_label_source",
                        columnNames = {"datasetCode", "versionLabel", "sourceSystem"}
                )
        },
        indexes = {
                @Index(name = "idx_dataset_version_current", columnList = "is_current"),
                @Index(name = "idx_dataset_version_collected_at", columnList = "collectedAt")
        })
public class DatasetVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String datasetCode;

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