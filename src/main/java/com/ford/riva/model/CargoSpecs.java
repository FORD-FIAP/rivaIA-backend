package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cargo_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CargoSpecs {

    @Id
    @Column(name = "version_id")
    private Integer versionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "version_id")
    private Version version;

    @Column(name = "cab_length", length = 30)
    private String cabLength;

    @Column(name = "cargo_capacity_kg")
    private Integer cargoCapacityKg;

    @Column(name = "towing_capacity_kg")
    private Integer towingCapacityKg;
}
