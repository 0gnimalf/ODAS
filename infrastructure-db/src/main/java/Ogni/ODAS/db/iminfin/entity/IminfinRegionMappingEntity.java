package Ogni.ODAS.db.iminfin.entity;

import Ogni.ODAS.db.entity.RegionEntity;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "iminfin_region_mapping", schema = "a",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_iminfin_region_mapping_source_external_code",
                        columnNames = {"source_system", "external_territory_code"}
                )
        },
        indexes = {
                @Index(name = "idx_iminfin_region_mapping_region", columnList = "region_id")
        })
public class IminfinRegionMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, length = 30)
    private String sourceSystem;

    @Column(name = "external_territory_code", nullable = false, length = 100)
    private String externalTerritoryCode;

    @Column(name = "raw_territory_name", length = 255)
    private String rawTerritoryName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RegionEntity region;
}
