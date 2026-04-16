package Ogni.ODAS.db.entity;

import Ogni.ODAS.domain.enumtype.FederalDistrictCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "region",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_region_code",
                columnNames = {"code"}
        ),
        indexes = {
                @Index(name = "idx_region_federal_district_code", columnList = "federal_district_code"),
                @Index(name = "idx_region_name", columnList = "name")
        }
)
public class RegionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "region_seq_gen")
    @SequenceGenerator(name = "region_seq_gen", sequenceName = "region_seq", allocationSize = 1)
    private Long id;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "federal_district_code", nullable = false, length = 16)
    private FederalDistrictCode federalDistrictCode;
}
