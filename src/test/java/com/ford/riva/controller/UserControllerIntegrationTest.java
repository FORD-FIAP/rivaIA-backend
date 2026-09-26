package com.ford.riva.controller;

import com.ford.riva.crypto.EmailHasher;
import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import com.ford.riva.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailHasher emailHasher;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User createUser(String username, Role role) {
        String email = username + "@example.com";
        User user = User.builder()
                .username(username)
                .email(email)
                .emailHash(emailHasher.hash(email))
                .password(passwordEncoder.encode("SenhaForte1"))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole());
    }

    @Test
    @DisplayName("ADMIN consegue anonimizar (excluir) um usuário via DELETE /api/v1/users/{id}")
    void adminCanDeleteUser() throws Exception {
        User admin = createUser("admin_del_test", Role.ADMIN);
        User target = createUser("target_to_delete", Role.USER);

        mockMvc.perform(delete("/api/v1/users/" + target.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isNoContent());

        User reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getUsername()).isEqualTo("deleted_user_" + target.getId());
        assertThat(reloaded.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("USER autenticado não consegue excluir usuário (403)")
    void userCannotDeleteUser() throws Exception {
        User regular = createUser("regular_del_test", Role.USER);
        User target = createUser("other_user_del_test", Role.USER);

        mockMvc.perform(delete("/api/v1/users/" + target.getId())
                        .header("Authorization", "Bearer " + tokenFor(regular)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE de usuário inexistente retorna 404")
    void deletingUnknownUserReturns404() throws Exception {
        User admin = createUser("admin_del_404_test", Role.ADMIN);

        mockMvc.perform(delete("/api/v1/users/999999")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isNotFound());
    }
}
