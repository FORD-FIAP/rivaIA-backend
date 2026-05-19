package com.ford.riva.repository;

import com.ford.riva.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByUsername(String username);

    boolean existsByEmailHash(String emailHash);
}
