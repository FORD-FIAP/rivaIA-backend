package com.ford.riva.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.riva.config.RateLimitConfig;
import com.ford.riva.dto.error.ApiErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final double WARNING_THRESHOLD = 0.2;

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        boolean loginAttempt = isLoginEndpoint(request);

        Bucket bucket;
        int capacity;
        if (loginAttempt) {
            bucket = authBuckets.computeIfAbsent(clientIp, ip -> rateLimitConfig.newAuthBucket());
            capacity = rateLimitConfig.getAuthLimit();
        } else {
            bucket = generalBuckets.computeIfAbsent(clientIp, ip -> rateLimitConfig.newGeneralBucket());
            capacity = rateLimitConfig.getGeneralLimit();
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            long remaining = probe.getRemainingTokens();
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));

            if (remaining <= capacity * WARNING_THRESHOLD) {
                log.warn("IP {} atingiu {}% do rate limit ({} de {} restantes) em {}",
                        clientIp, (int) ((1 - WARNING_THRESHOLD) * 100), remaining, capacity,
                        request.getRequestURI());
            }
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
            log.warn("Rate limit excedido para IP {} em {} ({})",
                    clientIp, request.getRequestURI(), loginAttempt ? "login" : "geral");
            writeTooManyRequests(request, response, retryAfterSeconds);
        }
    }

    private boolean isLoginEndpoint(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Limite de requisições excedido. Tente novamente mais tarde.")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.addHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getWriter(), body);
    }
}
