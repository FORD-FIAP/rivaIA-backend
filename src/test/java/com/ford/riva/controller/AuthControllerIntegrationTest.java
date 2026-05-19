package com.ford.riva.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.riva.dto.auth.LoginRequest;
import com.ford.riva.dto.auth.RefreshTokenRequest;
import com.ford.riva.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    @DisplayName("POST /register cria usuário e retorna access + refresh token")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("newuser", "new@example.com", "SenhaForte1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("POST /register com username duplicado retorna 400")
    void registerDuplicateUsername() throws Exception {
        RegisterRequest req = new RegisterRequest("dupuser", "first@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated());

        RegisterRequest dup = new RegisterRequest("dupuser", "other@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(dup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Username")));
    }

    @Test
    @DisplayName("POST /register com email duplicado retorna 400 (blind index funciona)")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest first = new RegisterRequest("user1", "shared@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest("user2", "shared@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email")));
    }

    @Test
    @DisplayName("POST /register normaliza case do email (blind index)")
    void registerEmailCaseInsensitive() throws Exception {
        RegisterRequest first = new RegisterRequest("usercase1", "Case@Example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest("usercase2", "CASE@example.COM", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(second)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register com payload inválido retorna 400 com map de erros")
    void registerValidationFails() throws Exception {
        String invalid = "{\"username\":\"\",\"email\":\"notanemail\",\"password\":\"short\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /register com JSON malformado retorna 400 (não 500)")
    void registerMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /login com credenciais válidas retorna tokens")
    void loginSuccess() throws Exception {
        RegisterRequest reg = new RegisterRequest("loginuser", "login@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("loginuser", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /login com senha errada retorna 401 com JSON padronizado")
    void loginWrongPassword() throws Exception {
        RegisterRequest reg = new RegisterRequest("wrongpwd", "wp@example.com", "SenhaForte1");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("wrongpwd", "WrongPassword1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /login de usuário inexistente retorna 401 (sem vazar que user não existe)")
    void loginNonexistentUser() throws Exception {
        LoginRequest login = new LoginRequest("ghostuser", "AnyPassword1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /refresh com refresh token válido retorna novos tokens")
    void refreshSuccess() throws Exception {
        RegisterRequest reg = new RegisterRequest("refreshuser", "refresh@example.com", "SenhaForte1");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String refreshToken = body.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /refresh com access token (não refresh) é rejeitado")
    void refreshRejectsAccessToken() throws Exception {
        RegisterRequest reg = new RegisterRequest("noaccess", "noac@example.com", "SenhaForte1");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = body.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest(accessToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /refresh com token lixo retorna 401")
    void refreshGarbage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest("garbage.token.value"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("access token retornado é JWT válido (3 partes separadas por ponto)")
    void accessTokenIsValidJwt() throws Exception {
        RegisterRequest reg = new RegisterRequest("jwtuser", "jwt@example.com", "SenhaForte1");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = body.get("accessToken").asText();

        assertThat(accessToken.split("\\.")).hasSize(3);
    }
}
