package com.ford.riva.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailHasherTest {

    private static final String SECRET = "testEmailHashSecretAtLeast32CharsLong!";

    private EmailHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new EmailHasher(SECRET);
    }

    @Test
    @DisplayName("rejeita secret muito curto")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new EmailHasher("short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }

    @Test
    @DisplayName("rejeita secret null")
    void rejectsNullSecret() {
        assertThatThrownBy(() -> new EmailHasher(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("hash é determinístico — mesmo input produz mesmo output")
    void isDeterministic() {
        String email = "vitoka@example.com";
        assertThat(hasher.hash(email)).isEqualTo(hasher.hash(email));
    }

    @Test
    @DisplayName("normaliza caso — VITOKA@EXAMPLE.com == vitoka@example.com")
    void normalizesCase() {
        assertThat(hasher.hash("VITOKA@EXAMPLE.COM"))
                .isEqualTo(hasher.hash("vitoka@example.com"));
    }

    @Test
    @DisplayName("normaliza whitespace — '  email  ' == 'email'")
    void normalizesWhitespace() {
        assertThat(hasher.hash("  vitoka@example.com  "))
                .isEqualTo(hasher.hash("vitoka@example.com"));
    }

    @Test
    @DisplayName("emails diferentes produzem hashes diferentes")
    void differentEmailsDifferentHashes() {
        assertThat(hasher.hash("a@example.com"))
                .isNotEqualTo(hasher.hash("b@example.com"));
    }

    @Test
    @DisplayName("secret diferente produz hash diferente para mesmo email")
    void differentSecretsDifferentHashes() {
        EmailHasher other = new EmailHasher("otherSecretAtLeast32CharsLongForTest!");
        assertThat(hasher.hash("vitoka@example.com"))
                .isNotEqualTo(other.hash("vitoka@example.com"));
    }

    @Test
    @DisplayName("null entra null sai")
    void nullPassesThrough() {
        assertThat(hasher.hash(null)).isNull();
    }

    @Test
    @DisplayName("hash tem tamanho fixo em Base64 (44 chars para 32 bytes)")
    void fixedSizeBase64() {
        String hash = hasher.hash("any@email.com");
        assertThat(hash).hasSize(44);
    }
}
