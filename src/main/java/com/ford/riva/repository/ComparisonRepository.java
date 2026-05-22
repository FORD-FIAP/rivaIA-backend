package com.ford.riva.repository;

import com.ford.riva.model.Comparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComparisonRepository extends JpaRepository<Comparison, Integer> {
    List<Comparison> findByUserIdOrderByCreatedAtDesc(Long userId);
}
