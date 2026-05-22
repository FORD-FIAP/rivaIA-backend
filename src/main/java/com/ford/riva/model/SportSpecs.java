package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sport_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportSpecs {

    @Id
    @Column(name = "version_id")
    private Integer versionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "version_id")
    private Version version;

    @Column(name = "acceleration_0_100", precision = 4, scale = 1)
    private BigDecimal acceleration0100;

    @Column(name = "brake_type", length = 50)
    private String brakeType;

    @Column(name = "driving_mode", length = 100)
    private String drivingMode;

    @Column(nullable = false)
    private Boolean convertible;
}
