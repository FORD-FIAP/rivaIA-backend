package com.ford.riva.service;

import com.ford.riva.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRetentionJob {

    private final AuditLogRepository auditLogRepository;

    @Value("${audit.retention.days:90}")
    private int retentionDays;

    @Scheduled(cron = "${audit.retention.cron:0 0 3 * * *}")
    @Transactional
    public void anonymizeOldEntries() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int affected = auditLogRepository.anonymizeEntriesOlderThan(cutoff);
        if (affected > 0) {
            log.info("Trilha de auditoria: {} entradas anteriores a {} foram anonimizadas (retencao={} dias)",
                    affected, cutoff, retentionDays);
        }
    }
}
