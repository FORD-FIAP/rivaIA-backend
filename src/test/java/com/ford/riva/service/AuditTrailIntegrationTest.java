package com.ford.riva.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.riva.dto.auth.LoginRequest;
import com.ford.riva.dto.auth.RegisterRequest;
import com.ford.riva.model.AuditAction;
import com.ford.riva.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanAuditTrail() {
        auditLogRepository.deleteAll();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(username, email, "SenhaForte1"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("registro de usuário gera entrada de auditoria USER_CREATED")
    void registerCreatesAuditEntry() throws Exception {
        register("audit_reg", "audit_reg@example.com");

        assertThat(auditLogRepository.findByAction(AuditAction.USER_CREATED))
                .anyMatch(entry -> entry.getDetails().contains("audit_reg"));
    }

    @Test
    @DisplayName("login bem-sucedido gera entrada de auditoria LOGIN")
    void loginCreatesAuditEntry() throws Exception {
        register("audit_login", "audit_login@example.com");
        auditLogRepository.deleteAll();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("audit_login", "SenhaForte1"))))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findByAction(AuditAction.LOGIN))
                .anyMatch(entry -> entry.getDetails().contains("audit_login"));
    }

    @Test
    @DisplayName("login com senha errada gera entrada de auditoria LOGIN_FAILED")
    void failedLoginCreatesAuditEntry() throws Exception {
        register("audit_fail", "audit_fail@example.com");
        auditLogRepository.deleteAll();

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("audit_fail", "SenhaErrada9"))))
                .andExpect(status().isUnauthorized());

        assertThat(auditLogRepository.findByAction(AuditAction.LOGIN_FAILED))
                .isNotEmpty();
    }

    @Test
    @DisplayName("5 falhas de login do mesmo IP disparam log ERROR de brute force")
    void bruteForceTriggersErrorLog() throws Exception {
        Logger authLogger = (Logger) LoggerFactory.getLogger(AuthService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        authLogger.addAppender(appender);

        try {
            String bruteIp = "198.51.100.123";
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                                .header("X-Forwarded-For", bruteIp)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(new LoginRequest("ghostuser", "WrongPass" + i))))
                        .andExpect(status().isUnauthorized());
            }

            boolean bruteForceLogged = appender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains("brute force"));
            assertThat(bruteForceLogged)
                    .as("deve registrar log ERROR de brute force após 5 falhas")
                    .isTrue();
        } finally {
            authLogger.detachAppender(appender);
        }
    }
}
