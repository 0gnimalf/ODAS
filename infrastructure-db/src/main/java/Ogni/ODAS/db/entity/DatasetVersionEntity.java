package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.SourceSystemCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dataset_version",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dataset_version_identity",
                columnNames = {"source_system_code", "external_title", "external_date_modified"}
        )
)
public class DatasetVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dataset_version_seq_gen")
    @SequenceGenerator(name = "dataset_version_seq_gen", sequenceName = "dataset_version_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system_code", nullable = false, length = 64)
    private SourceSystemCode sourceSystemCode;

    @Column(name = "external_title", nullable = false, length = 500)
    private String externalTitle;

    @Column(name = "external_date_modified", nullable = false)
    private OffsetDateTime externalDateModified;
}
