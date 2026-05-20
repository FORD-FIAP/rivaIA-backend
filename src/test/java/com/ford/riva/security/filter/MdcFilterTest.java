package com.ford.riva.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("popula trace_id e client_ip no MDC durante a requisição")
    void populatesMdcDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> captured = new HashMap<>();
        FilterChain chain = (req, res) -> {
            captured.put(MdcFilter.TRACE_ID, MDC.get(MdcFilter.TRACE_ID));
            captured.put(MdcFilter.CLIENT_IP, MDC.get(MdcFilter.CLIENT_IP));
        };

        filter.doFilter(request, response, chain);

        assertThat(captured.get(MdcFilter.TRACE_ID)).isNotNull().hasSize(8);
        assertThat(captured.get(MdcFilter.CLIENT_IP)).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("MDC é limpo após o término da requisição")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.11");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(MdcFilter.TRACE_ID)).isNull();
        assertThat(MDC.get(MdcFilter.CLIENT_IP)).isNull();
        assertThat(MDC.get(MdcFilter.USER_ID)).isNull();
    }

    @Test
    @DisplayName("usa o primeiro IP do header X-Forwarded-For quando presente")
    void usesForwardedForHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> captured = new HashMap<>();
        FilterChain chain = (req, res) -> captured.put(
                MdcFilter.CLIENT_IP, MDC.get(MdcFilter.CLIENT_IP));

        filter.doFilter(request, response, chain);

        assertThat(captured.get(MdcFilter.CLIENT_IP)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("MDC é limpo mesmo se a cadeia lançar exceção")
    void clearsMdcEvenOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.12");
        FilterChain failingChain = (req, res) -> {
            throw new RuntimeException("falha simulada");
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), failingChain);
        } catch (Exception ignored) {
            // esperado
        }

        assertThat(MDC.get(MdcFilter.TRACE_ID)).isNull();
        assertThat(MDC.get(MdcFilter.CLIENT_IP)).isNull();
    }
}
