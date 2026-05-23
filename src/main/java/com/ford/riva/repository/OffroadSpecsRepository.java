package com.ford.riva.repository;

import com.ford.riva.model.OffroadSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OffroadSpecsRepository extends JpaRepository<OffroadSpecs, Integer> {}
