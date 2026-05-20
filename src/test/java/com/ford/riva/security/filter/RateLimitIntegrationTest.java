package com.ford.riva.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "rate-limit.general.requests-per-minute=5",
        "rate-limit.auth.requests-per-minute=3"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("requests dentro do limite geral passam normalmente")
    void withinGeneralLimitPasses() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", "10.0.0.1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("excedendo o limite geral retorna 429 com Retry-After")
    void exceedingGeneralLimitReturns429() throws Exception {
        String ip = "10.0.0.2";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", ip))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Limite de requisições")));
    }

    @Test
    @DisplayName("limite de login (anti brute-force) é mais restrito que o geral")
    void loginLimitIsStricter() throws Exception {
        String ip = "10.0.0.3";
        String body = "{\"username\":\"ghost\",\"password\":\"WrongPass123\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("buckets são isolados por IP — um IP esgotado não afeta outro")
    void bucketsAreIsolatedPerIp() throws Exception {
        String exhaustedIp = "10.0.0.4";
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", exhaustedIp));
        }
        mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", exhaustedIp))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", "10.0.0.99"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("response inclui header X-Rate-Limit-Remaining")
    void responseExposesRemainingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/users/x").header("X-Forwarded-For", "10.0.0.5"))
                .andExpect(header().exists("X-Rate-Limit-Remaining"));
    }
}
