package com.ford.riva.repository;

import com.ford.riva.model.Search;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<Search, Integer> {
    List<Search> findByUserIdOrderByCreatedAtDesc(Long userId);
}
