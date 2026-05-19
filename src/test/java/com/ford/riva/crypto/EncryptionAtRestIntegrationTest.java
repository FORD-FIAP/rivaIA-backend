package com.ford.riva.crypto;

import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EncryptionAtRestIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailHasher emailHasher;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Email é persistido CRIPTOGRAFADO no banco (não em plaintext)")
    void emailIsActuallyEncryptedInDatabase() {
        String plaintextEmail = "victim@example.com";
        User user = User.builder()
                .username("encryptiontest")
                .email(plaintextEmail)
                .emailHash(emailHasher.hash(plaintextEmail))
                .password("$2a$12$fakebcrypthash")
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.saveAndFlush(user);
        entityManager.clear();

        Object raw = entityManager.createNativeQuery(
                        "SELECT email FROM users WHERE username = :u")
                .setParameter("u", "encryptiontest")
                .getSingleResult();

        String dbValue = raw.toString();

        assertThat(dbValue)
                .as("email no banco NUNCA pode conter o plaintext")
                .doesNotContain(plaintextEmail)
                .doesNotContain("victim");

        assertThat(dbValue)
                .as("email no banco deve estar em Base64 (formato do ciphertext)")
                .matches("^[A-Za-z0-9+/=]+$");

        assertThat(dbValue.length())
                .as("ciphertext é maior que o plaintext (IV + tag + base64 overhead)")
                .isGreaterThan(plaintextEmail.length());
    }

    @Test
    @DisplayName("Email é decifrado AUTOMATICAMENTE ao carregar via repository")
    void emailIsDecryptedOnRead() {
        String plaintextEmail = "roundtrip@example.com";
        User saved = userRepository.saveAndFlush(User.builder()
                .username("roundtriptest")
                .email(plaintextEmail)
                .emailHash(emailHasher.hash(plaintextEmail))
                .password("$2a$12$x")
                .role(Role.USER)
                .enabled(true)
                .build());
        Long id = saved.getId();
        entityManager.clear();

        User loaded = userRepository.findById(id).orElseThrow();

        assertThat(loaded.getEmail())
                .as("email decifrado deve bater com o original")
                .isEqualTo(plaintextEmail);
    }

    @Test
    @DisplayName("Mesmo email salvo duas vezes produz ciphertexts diferentes na DB (IV aleatório)")
    void sameEmailDifferentCiphertexts() {
        String email = "duplicate-check@example.com";
        User u1 = userRepository.saveAndFlush(User.builder()
                .username("dup1")
                .email(email)
                .emailHash(emailHasher.hash(email + "_1"))
                .password("$2a$12$x")
                .role(Role.USER).enabled(true)
                .build());
        User u2 = userRepository.saveAndFlush(User.builder()
                .username("dup2")
                .email(email)
                .emailHash(emailHasher.hash(email + "_2"))
                .password("$2a$12$x")
                .role(Role.USER).enabled(true)
                .build());
        entityManager.clear();

        Object c1 = entityManager.createNativeQuery("SELECT email FROM users WHERE user_id = :id")
                .setParameter("id", u1.getId()).getSingleResult();
        Object c2 = entityManager.createNativeQuery("SELECT email FROM users WHERE user_id = :id")
                .setParameter("id", u2.getId()).getSingleResult();

        assertThat(c1.toString())
                .as("AES-GCM com IV aleatório nunca produz mesmo ciphertext")
                .isNotEqualTo(c2.toString());
    }

    @Test
    @DisplayName("findByEmailHash localiza usuário pelo blind index (sem decifrar todos os emails)")
    void blindIndexLookupWorks() {
        String email = "findme@example.com";
        userRepository.saveAndFlush(User.builder()
                .username("findmeuser")
                .email(email)
                .emailHash(emailHasher.hash(email))
                .password("$2a$12$x")
                .role(Role.USER).enabled(true)
                .build());
        entityManager.clear();

        Optional<User> found = userRepository.findByEmailHash(emailHasher.hash(email));
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);

        Optional<User> foundCaseInsensitive = userRepository.findByEmailHash(emailHasher.hash("FINDME@example.COM"));
        assertThat(foundCaseInsensitive).isPresent();
    }
}
