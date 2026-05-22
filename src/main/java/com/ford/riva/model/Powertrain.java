package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "powertrains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Powertrain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "powertrain_id")
    private Integer powertrainId;

    @Column(nullable = false, length = 100)
    private String engine;

    @Column(name = "power_hp", nullable = false)
    private Integer powerHp;

    @Column(name = "torque_nm", nullable = false)
    private Integer torqueNm;

    @Column(nullable = false, length = 50)
    private String transmission;

    @Column(nullable = false, length = 20)
    private String drivetrain;

    @Column(name = "tank_liters", nullable = false)
    private Integer tankLiters;

    @Column(name = "fuel_type", nullable = false, length = 30)
    private String fuelType;

    @OneToMany(mappedBy = "powertrain")
    @Builder.Default
    private List<Version> versions = new ArrayList<>();
}
