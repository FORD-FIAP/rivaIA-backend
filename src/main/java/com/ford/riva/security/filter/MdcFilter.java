package com.ford.riva.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Popula o MDC (Mapped Diagnostic Context) com identificadores de contexto da
 * requisição, para que todos os logs gerados durante o request sejam
 * correlacionáveis. Roda primeiro na cadeia de filtros de segurança.
 */
@Component
public class MdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "trace_id";
    public static final String CLIENT_IP = "client_ip";
    public static final String USER_ID = "user_id";

    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final int TRACE_ID_LENGTH = 8;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            MDC.put(TRACE_ID, generateTraceId());
            MDC.put(CLIENT_IP, resolveClientIp(request));

            String userId = resolveUserId();
            if (userId != null) {
                MDC.put(USER_ID, userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, TRACE_ID_LENGTH);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return null;
    }
}
