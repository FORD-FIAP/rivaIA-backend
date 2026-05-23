package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Integer versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "powertrain_id", nullable = false)
    private Powertrain powertrain;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean sunroof;

    @OneToOne(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private Dimensions dimensions;

    @OneToOne(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private SafetyTech safetyTech;

    @OneToOne(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private SportSpecs sportSpecs;

    @OneToOne(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private OffroadSpecs offroadSpecs;

    @OneToOne(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private CargoSpecs cargoSpecs;

    @ManyToMany(mappedBy = "versions")
    @Builder.Default
    private Set<Comparison> comparisons = new HashSet<>();
}
