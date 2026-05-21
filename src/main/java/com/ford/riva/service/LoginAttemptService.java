package com.ford.riva.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rastreia falhas de login por IP dentro de uma janela de tempo, para
 * detecção de tentativas de brute force.
 */
@Service
public class LoginAttemptService {

    private static final int SUSPICIOUS_THRESHOLD = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, Deque<Instant>> failuresByIp = new ConcurrentHashMap<>();

    /**
     * Registra uma falha de login para o IP e retorna o número de falhas
     * dentro da janela de tempo atual.
     */
    public int recordFailure(String ip) {
        if (ip == null || ip.isBlank()) {
            return 0;
        }
        Instant now = Instant.now();
        Deque<Instant> failures = failuresByIp.computeIfAbsent(ip, key -> new ConcurrentLinkedDeque<>());
        failures.addLast(now);
        purgeExpired(failures, now);
        return failures.size();
    }

    /** Indica se o IP atingiu o limiar de falhas considerado suspeito. */
    public boolean isSuspicious(String ip) {
        if (ip == null) {
            return false;
        }
        Deque<Instant> failures = failuresByIp.get(ip);
        if (failures == null) {
            return false;
        }
        purgeExpired(failures, Instant.now());
        return failures.size() >= SUSPICIOUS_THRESHOLD;
    }

    /** Limpa o histórico de falhas do IP (chamado após um login bem-sucedido). */
    public void reset(String ip) {
        if (ip != null) {
            failuresByIp.remove(ip);
        }
    }

    public int getSuspiciousThreshold() {
        return SUSPICIOUS_THRESHOLD;
    }

    private void purgeExpired(Deque<Instant> failures, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        failures.removeIf(timestamp -> timestamp.isBefore(cutoff));
    }
}
