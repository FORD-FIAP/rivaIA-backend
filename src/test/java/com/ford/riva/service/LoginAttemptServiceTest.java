package com.ford.riva.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    @DisplayName("recordFailure incrementa a contagem do IP")
    void recordFailureIncrements() {
        assertThat(service.recordFailure("10.0.0.1")).isEqualTo(1);
        assertThat(service.recordFailure("10.0.0.1")).isEqualTo(2);
        assertThat(service.recordFailure("10.0.0.1")).isEqualTo(3);
    }

    @Test
    @DisplayName("IP não é suspeito abaixo do limiar")
    void notSuspiciousBelowThreshold() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("10.0.0.2");
        }
        assertThat(service.isSuspicious("10.0.0.2")).isFalse();
    }

    @Test
    @DisplayName("IP vira suspeito ao atingir 5 falhas")
    void suspiciousAtThreshold() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("10.0.0.3");
        }
        assertThat(service.isSuspicious("10.0.0.3")).isTrue();
    }

    @Test
    @DisplayName("reset limpa o histórico de falhas do IP")
    void resetClearsHistory() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("10.0.0.4");
        }
        service.reset("10.0.0.4");
        assertThat(service.isSuspicious("10.0.0.4")).isFalse();
        assertThat(service.recordFailure("10.0.0.4")).isEqualTo(1);
    }

    @Test
    @DisplayName("falhas são contabilizadas isoladamente por IP")
    void failuresIsolatedPerIp() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("10.0.0.5");
        }
        assertThat(service.isSuspicious("10.0.0.5")).isTrue();
        assertThat(service.isSuspicious("10.0.0.6")).isFalse();
        assertThat(service.recordFailure("10.0.0.6")).isEqualTo(1);
    }

    @Test
    @DisplayName("IP null ou vazio é tratado com segurança")
    void nullAndBlankIpAreSafe() {
        assertThat(service.recordFailure(null)).isZero();
        assertThat(service.recordFailure("")).isZero();
        assertThat(service.isSuspicious(null)).isFalse();
    }

    @Test
    @DisplayName("IP desconhecido não é suspeito")
    void unknownIpNotSuspicious() {
        assertThat(service.isSuspicious("203.0.113.200")).isFalse();
    }

    @Test
    @DisplayName("limiar exposto é 5")
    void thresholdIsFive() {
        assertThat(service.getSuspiciousThreshold()).isEqualTo(5);
    }
}
