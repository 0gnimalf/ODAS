package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "region", schema = "a",
        indexes = {
                @Index(name = "idx_region_name", columnList = "name")
        })
public class RegionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FederalDistrictCode federalDistrictCode;
}