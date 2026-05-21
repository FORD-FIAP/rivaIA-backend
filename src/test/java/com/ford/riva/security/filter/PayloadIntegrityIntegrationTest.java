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
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "security.hmac.enabled=true",
        "security.hmac.secret=integrationTestHmacSecretAtLeast32Chars!"
})
class PayloadIntegrityIntegrationTest {

    private static final String HMAC_SECRET = "integrationTestHmacSecretAtLeast32Chars!";

    @Autowired
    private MockMvc mockMvc;

    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("POST sem header X-Signature é rejeitado com 403")
    void postWithoutSignatureRejected() throws Exception {
        String body = "{\"username\":\"hmacuser1\",\"email\":\"hmac1@example.com\",\"password\":\"SenhaForte1\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("integridade")));
    }

    @Test
    @DisplayName("POST com assinatura incorreta é rejeitado com 403")
    void postWithWrongSignatureRejected() throws Exception {
        String body = "{\"username\":\"hmacuser2\",\"email\":\"hmac2@example.com\",\"password\":\"SenhaForte1\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Signature", "assinatura-falsa-qualquer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST com assinatura HMAC correta passa normalmente")
    void postWithValidSignatureAccepted() throws Exception {
        String body = "{\"username\":\"hmacuser3\",\"email\":\"hmac3@example.com\",\"password\":\"SenhaForte1\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Signature", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("body adulterado após assinar invalida o HMAC")
    void tamperedBodyInvalidatesSignature() throws Exception {
        String original = "{\"username\":\"hmacuser4\",\"email\":\"hmac4@example.com\",\"password\":\"SenhaForte1\"}";
        String signature = sign(original);
        String tampered = "{\"username\":\"attacker\",\"email\":\"hmac4@example.com\",\"password\":\"SenhaForte1\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tampered))
                .andExpect(status().isForbidden());
    }
}
