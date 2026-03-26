package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "region", schema = "a")
public class RegionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FederalDistrictCode federalDistrictCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}