package com.ford.riva.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;

@Component
public class EmailHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public EmailHasher(@Value("${email.hash.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "email.hash.secret deve ter pelo menos 32 caracteres para HMAC-SHA256"
            );
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] hashBytes = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao gerar hash do email", ex);
        }
    }
}
