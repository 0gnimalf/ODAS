package Ogni.ODAS.db.iminfin.entity;

import Ogni.ODAS.application.iminfin.model.IminfinDiscoveryStatus;
import Ogni.ODAS.application.iminfin.model.IminfinReportProfileKey;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "iminfin_discovery_state", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_iminfin_discovery_state_profile",
                        columnNames = {"profile_key"}
                )
        })
public class IminfinDiscoveryStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_key", nullable = false, length = 50)
    private IminfinReportProfileKey profileKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IminfinDiscoveryStatus status;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    @Column(name = "last_error_message", length = 2000)
    private String lastErrorMessage;
}
