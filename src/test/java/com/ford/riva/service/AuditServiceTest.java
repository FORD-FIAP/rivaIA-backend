package com.ford.riva.service;

import com.ford.riva.model.AuditAction;
import com.ford.riva.model.AuditLog;
import com.ford.riva.repository.AuditLogRepository;
import com.ford.riva.security.filter.MdcFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("log captura ação, recurso, usuário e IP do MDC")
    void logCapturesContext() {
        MDC.put(MdcFilter.USER_ID, "alice");
        MDC.put(MdcFilter.CLIENT_IP, "203.0.113.20");

        auditService.log(AuditAction.LOGIN, "/api/v1/auth/login", "username=alice");

        AuditLog saved = captureSaved();
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(saved.getResource()).isEqualTo("/api/v1/auth/login");
        assertThat(saved.getDetails()).isEqualTo("username=alice");
        assertThat(saved.getUserId()).isEqualTo("alice");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.20");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("log funciona sem contexto MDC (userId e ip ficam null)")
    void logWorksWithoutMdc() {
        auditService.log(AuditAction.LOGIN_FAILED, "/api/v1/auth/login", "username=ghost");

        AuditLog saved = captureSaved();
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN_FAILED);
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getIpAddress()).isNull();
    }

    @Test
    @DisplayName("details longos são truncados em 500 caracteres")
    void truncatesLongDetails() {
        String longDetails = "x".repeat(600);

        auditService.log(AuditAction.MASS_QUERY, "/api/v1/vehicles", longDetails);

        AuditLog saved = captureSaved();
        assertThat(saved.getDetails()).hasSize(500);
    }
}
