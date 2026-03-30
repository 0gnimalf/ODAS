package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "region", schema = "a",
        indexes = {
                @Index(name = "idx_region_name", columnList = "name")
        })
public class RegionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FederalDistrictCode federalDistrictCode;
}