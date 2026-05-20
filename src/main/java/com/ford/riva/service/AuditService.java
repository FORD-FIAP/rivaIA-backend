package com.ford.riva.service;

import com.ford.riva.model.AuditAction;
import com.ford.riva.model.AuditLog;
import com.ford.riva.repository.AuditLogRepository;
import com.ford.riva.security.filter.MdcFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Registra uma entrada na trilha de auditoria, capturando o contexto da
     * requisição (usuário e IP) a partir do MDC.
     *
     * <p>Executa em uma transação independente (REQUIRES_NEW) para que a
     * trilha de auditoria persista mesmo que a transação de negócio falhe.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String resource, String details) {
        AuditLog entry = AuditLog.builder()
                .timestamp(Instant.now())
                .userId(MDC.get(MdcFilter.USER_ID))
                .action(action)
                .resource(resource)
                .ipAddress(MDC.get(MdcFilter.CLIENT_IP))
                .details(truncate(details))
                .build();

        auditLogRepository.save(entry);
        log.info("Auditoria: action={} resource={}", action, resource);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
