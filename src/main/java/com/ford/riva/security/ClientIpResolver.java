package com.ford.riva.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Resolve o IP real do cliente considerando {@code X-Forwarded-For} apenas
 * quando a conexão direta (getRemoteAddr) vem de uma rede interna/confiável
 * (loopback, RFC1918, link-local) — o mesmo critério do
 * {@code internalProxies} padrão do Tomcat/RemoteIpValve.
 *
 * Sem essa checagem, qualquer cliente externo poderia forjar o header
 * X-Forwarded-For para burlar rate limiting e poluir a trilha de auditoria
 * com IPs falsos.
 */
@Component
public class ClientIpResolver {

    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private static final Pattern TRUSTED_PROXY = Pattern.compile(
            "127\\..*|::1|0:0:0:0:0:0:0:1"
                    + "|10\\..*|192\\.168\\..*"
                    + "|172\\.(1[6-9]|2\\d|3[01])\\..*"
                    + "|169\\.254\\..*"
    );

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);

        if (remoteAddr != null && TRUSTED_PROXY.matcher(remoteAddr).matches()
                && StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
