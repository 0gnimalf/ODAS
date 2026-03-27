package Ogni.ODAS.db.iminfin.entity;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryStatus;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "iminfin_report_session", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_iminfin_report_session_profile_data_version",
                        columnNames = {"profile_key", "data_version"}
                )
        },
        indexes = {
                @Index(name = "idx_iminfin_report_session_profile", columnList = "profile_key"),
                @Index(name = "idx_iminfin_report_session_resolved_at", columnList = "resolved_at")
        })
public class IminfinReportSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_key", nullable = false, length = 50)
    private IminfinReportProfileKey profileKey;

    @Column(name = "report_id", length = 100)
    private String reportId;

    @Column(length = 100)
    private String uuid;

    @Column(name = "version_label", length = 100)
    private String versionLabel;

    @Column(name = "data_version", nullable = false, length = 100)
    private String dataVersion;

    @Column(name = "resolved_at", nullable = false)
    private OffsetDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IminfinDiscoveryStatus status;
}
