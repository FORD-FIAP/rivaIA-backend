package com.ford.riva.repository;

import com.ford.riva.model.AuditAction;
import com.ford.riva.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAction(AuditAction action);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByTimestampBefore(Instant cutoff);
}
