package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dimensions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dimensions {

    @Id
    @Column(name = "version_id")
    private Integer versionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "version_id")
    private Version version;

    @Column(name = "length_mm", nullable = false)
    private Integer lengthMm;

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @Column(name = "wheelbase_mm", nullable = false)
    private Integer wheelbaseMm;

    @Column(name = "ground_clearance_mm", nullable = false)
    private Integer groundClearanceMm;
}
