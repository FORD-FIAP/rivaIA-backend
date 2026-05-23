package com.ford.riva.repository;

import com.ford.riva.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer>, JpaSpecificationExecutor<Vehicle> {
    List<Vehicle> findByBrandBrandId(Integer brandId);
    List<Vehicle> findByCategoryCategoryId(Integer categoryId);
    List<Vehicle> findByModelContainingIgnoreCase(String model);
    List<Vehicle> findByModelYear(Integer modelYear);
}
