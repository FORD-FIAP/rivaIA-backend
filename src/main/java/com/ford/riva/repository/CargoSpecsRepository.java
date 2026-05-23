package com.ford.riva.repository;

import com.ford.riva.model.CargoSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CargoSpecsRepository extends JpaRepository<CargoSpecs, Integer> {}
