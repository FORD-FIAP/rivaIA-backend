package com.ford.riva.repository;

import com.ford.riva.model.SportSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportSpecsRepository extends JpaRepository<SportSpecs, Integer> {}
