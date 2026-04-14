package com.ford.riva.repository;

import com.ford.riva.model.ExemploModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExemploRepository extends JpaRepository<ExemploModel, Long> {
}
