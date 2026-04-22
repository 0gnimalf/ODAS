package Ogni.ODAS.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dataset_collection",
        indexes = @Index(
                name = "idx_dataset_collection_version_collected_at",
                columnList = "dataset_version_id, collected_at"
        )
)
public class DatasetCollectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dataset_collection_seq_gen")
    @SequenceGenerator(name = "dataset_collection_seq_gen", sequenceName = "dataset_collection_seq", allocationSize = 1)
    private Long id;

    @Column(name = "dataset_version_id", nullable = false)
    private Long datasetVersionId;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Column(name = "request", nullable = false, length = 4000)
    private String request;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode rawData;
}
