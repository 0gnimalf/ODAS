package Ogni.ODAS.db.iminfin.entity;

import Ogni.ODAS.application.iminfin.model.IminfinOptionVerificationStatus;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "iminfin_parameter_option", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_iminfin_parameter_option_profile_parameter_value",
                        columnNames = {"profile_key", "parameter_name", "external_value"}
                )
        },
        indexes = {
                @Index(name = "idx_iminfin_parameter_option_profile_parameter", columnList = "profile_key, parameter_name")
        })
public class IminfinParameterOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_key", nullable = false, length = 50)
    private IminfinReportProfileKey profileKey;

    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;

    @Column(name = "external_value", nullable = false, length = 100)
    private String externalValue;

    @Column(name = "raw_label", length = 255)
    private String rawLabel;

    @Column(name = "internal_option_code", length = 100)
    private String internalOptionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private IminfinOptionVerificationStatus verificationStatus;

    @Column(name = "discovered_at", nullable = false)
    private OffsetDateTime discoveredAt;
}
