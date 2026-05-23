package com.ford.riva.repository;

import com.ford.riva.model.AuditAction;
import com.ford.riva.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAction(AuditAction action);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByTimestampBefore(Instant cutoff);

    @Modifying
    @Query("UPDATE AuditLog a SET a.userId = NULL, a.ipAddress = NULL, a.details = NULL " +
            "WHERE a.timestamp < :cutoff AND (a.userId IS NOT NULL OR a.ipAddress IS NOT NULL OR a.details IS NOT NULL)")
    int anonymizeEntriesOlderThan(@Param("cutoff") Instant cutoff);
}
