package Ogni.ODAS.db.iminfin.entity;

import Ogni.ODAS.application.iminfin.model.IminfinParameterRole;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "iminfin_parameter_catalog", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_iminfin_parameter_catalog_profile_parameter",
                        columnNames = {"profile_key", "parameter_name"}
                )
        },
        indexes = {
                @Index(name = "idx_iminfin_parameter_catalog_profile", columnList = "profile_key")
        })
public class IminfinParameterCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_key", nullable = false, length = 50)
    private IminfinReportProfileKey profileKey;

    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IminfinParameterRole role;

    @Column(name = "value_type", nullable = false, length = 50)
    private String valueType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "default_value", length = 255)
    private String defaultValue;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;
}
