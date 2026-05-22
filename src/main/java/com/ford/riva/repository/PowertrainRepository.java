package com.ford.riva.repository;

import com.ford.riva.model.Powertrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PowertrainRepository extends JpaRepository<Powertrain, Integer> {
    List<Powertrain> findByFuelType(String fuelType);
    List<Powertrain> findByDrivetrain(String drivetrain);
    List<Powertrain> findByTransmission(String transmission);
}
