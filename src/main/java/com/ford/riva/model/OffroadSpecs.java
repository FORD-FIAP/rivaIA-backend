package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "offroad_specs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffroadSpecs {

    @Id
    @Column(name = "version_id")
    private Integer versionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "version_id")
    private Version version;

    @Column(name = "approach_angle")
    private Integer approachAngle;

    @Column(name = "departure_angle")
    private Integer departureAngle;

    @Column(name = "breakover_angle")
    private Integer breakoverAngle;

    @Column(name = "wading_depth")
    private Integer wadingDepth;

    @Column(name = "driving_modes", length = 200)
    private String drivingModes;

    @Column(name = "diff_lock", nullable = false)
    private Boolean diffLock;

    @Column(name = "hill_descent_control", nullable = false)
    private Boolean hillDescentControl;
}
