package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "safety_tech")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyTech {

    @Id
    @Column(name = "version_id")
    private Integer versionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "version_id")
    private Version version;

    @Column(name = "num_airbags", nullable = false)
    private Integer numAirbags;

    @Column(nullable = false)
    private Boolean abs;

    @Column(name = "stability_control", nullable = false)
    private Boolean stabilityControl;

    @Column(name = "brake_assist", nullable = false)
    private Boolean brakeAssist;

    @Column(name = "rear_camera", nullable = false)
    private Boolean rearCamera;

    @Column(name = "parking_sensor", length = 30)
    private String parkingSensor;

    @Column(name = "blind_spot_monitor", nullable = false)
    private Boolean blindSpotMonitor;

    @Column(length = 100)
    private String infotainment;

    @Column(name = "phone_connectivity", length = 100)
    private String phoneConnectivity;
}
