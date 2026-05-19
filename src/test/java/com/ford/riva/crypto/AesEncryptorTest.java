package com.ford.riva.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptorTest {

    private static final String VALID_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private AesEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesEncryptor(VALID_KEY_BASE64);
    }

    @Test
    @DisplayName("rejeita chave que não é Base64 válido")
    void rejectsInvalidBase64Key() {
        assertThatThrownBy(() -> new AesEncryptor("not_valid_base64!!@@##"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("rejeita chave com tamanho diferente de 256 bits")
    void rejectsWrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new AesEncryptor(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    @DisplayName("encriptação e decriptação roundtrip preservam o plaintext")
    void roundtripPreservesPlaintext() {
        String plaintext = "vitoka@example.com";
        String ciphertext = encryptor.convertToDatabaseColumn(plaintext);
        String decrypted = encryptor.convertToEntityAttribute(ciphertext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encriptação de mesma string produz ciphertexts diferentes (IV aleatório)")
    void encryptionIsNonDeterministic() {
        String plaintext = "same@email.com";
        String first = encryptor.convertToDatabaseColumn(plaintext);
        String second = encryptor.convertToDatabaseColumn(plaintext);
        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.convertToEntityAttribute(first)).isEqualTo(plaintext);
        assertThat(encryptor.convertToEntityAttribute(second)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("ciphertext adulterado falha na decriptação (GCM tag inválida)")
    void tamperedCiphertextFails() {
        String plaintext = "secret@example.com";
        String ciphertext = encryptor.convertToDatabaseColumn(plaintext);
        byte[] bytes = Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> encryptor.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("adulteração");
    }

    @Test
    @DisplayName("ciphertext encriptado com chave diferente não pode ser lido")
    void differentKeyCannotDecrypt() {
        byte[] otherKey = new byte[32];
        for (int i = 0; i < otherKey.length; i++) {
            otherKey[i] = (byte) i;
        }
        AesEncryptor otherEncryptor = new AesEncryptor(Base64.getEncoder().encodeToString(otherKey));

        String ciphertext = encryptor.convertToDatabaseColumn("secret@example.com");
        assertThatThrownBy(() -> otherEncryptor.convertToEntityAttribute(ciphertext))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("null entra e sai null")
    void nullPassesThrough() {
        assertThat(encryptor.convertToDatabaseColumn(null)).isNull();
        assertThat(encryptor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("ciphertext truncado falha (sem IV completo)")
    void truncatedCiphertextFails() {
        String truncated = Base64.getEncoder().encodeToString(new byte[4]);
        assertThatThrownBy(() -> encryptor.convertToEntityAttribute(truncated))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("preserva caracteres unicode (acentos, emojis)")
    void preservesUnicode() {
        String plaintext = "usuário@exemplo.com.br 🚗";
        String ciphertext = encryptor.convertToDatabaseColumn(plaintext);
        assertThat(encryptor.convertToEntityAttribute(ciphertext)).isEqualTo(plaintext);
    }
}
