package com.ford.riva.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.riva.dto.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Verifica a integridade do corpo de requisições POST/PUT via HMAC-SHA256.
 * O cliente deve enviar o header X-Signature = Base64(HMAC-SHA256(body, segredo)).
 * Desabilitável em dev via security.hmac.enabled=false.
 */
@Slf4j
@Component
public class PayloadIntegrityFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final boolean enabled;
    private final byte[] hmacSecret;
    private final ObjectMapper objectMapper;

    public PayloadIntegrityFilter(
            @Value("${security.hmac.enabled:false}") boolean enabled,
            @Value("${security.hmac.secret:}") String hmacSecret,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        if (enabled && hmacSecret.isBlank()) {
            throw new IllegalStateException(
                    "security.hmac.secret é obrigatório quando security.hmac.enabled=true"
            );
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!enabled || !isProtectedMethod(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        String expectedSignature = computeHmac(cachedRequest.getCachedBody());

        if (!StringUtils.hasText(providedSignature) || !constantTimeEquals(providedSignature, expectedSignature)) {
            log.warn("Assinatura HMAC inválida ou ausente em {} {}",
                    request.getMethod(), request.getRequestURI());
            writeForbidden(request, response);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private boolean isProtectedMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }

    private String computeHmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecret, HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(body));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao calcular HMAC do payload", ex);
        }
    }

    private boolean constantTimeEquals(String provided, String expected) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Assinatura de integridade do payload inválida")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
