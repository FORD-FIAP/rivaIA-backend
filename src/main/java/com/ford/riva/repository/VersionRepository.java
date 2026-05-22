package com.ford.riva.repository;

import com.ford.riva.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VersionRepository extends JpaRepository<Version, Integer> {
    List<Version> findByVehicleVehicleId(Integer vehicleId);
    List<Version> findByPowertrainPowertrainId(Integer powertrainId);
}
