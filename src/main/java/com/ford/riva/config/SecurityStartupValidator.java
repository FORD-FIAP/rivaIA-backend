package com.ford.riva.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Impede a subida da aplicação em produção com os segredos de
 * desenvolvimento (definidos como fallback em application.properties).
 *
 * Sem essa checagem, esquecer uma variável de ambiente (JWT_SECRET,
 * AES_ENCRYPTION_KEY, EMAIL_HASH_SECRET, HMAC_SECRET ou
 * ADMIN_DEFAULT_PASSWORD) faria o app subir normalmente em produção usando
 * um segredo conhecido publicamente (este repositório), permitindo forjar
 * tokens JWT válidos, descriptografar e-mails ou logar como admin.
 */
@Slf4j
@Component
public class SecurityStartupValidator {

    private static final String DEV_JWT_SECRET = "defaultDevSecretThatIsAtLeast256BitsLongForHS256Algorithm!!";
    private static final String DEV_ADMIN_PASSWORD = "Admin@123456";
    private static final String DEV_AES_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DEV_EMAIL_HASH_SECRET = "devEmailHashSecretAtLeast32CharsLong!";
    private static final String DEV_HMAC_SECRET = "devHmacSecretAtLeast32CharsLongForDev!";

    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${admin.default.password}")
    private String adminDefaultPassword;

    @Value("${aes.encryption.key}")
    private String aesEncryptionKey;

    @Value("${email.hash.secret}")
    private String emailHashSecret;

    @Value("${security.hmac.enabled:false}")
    private boolean hmacEnabled;

    @Value("${security.hmac.secret:}")
    private String hmacSecret;

    public SecurityStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (!List.of(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        require(!DEV_JWT_SECRET.equals(jwtSecret), "JWT_SECRET");
        require(!DEV_ADMIN_PASSWORD.equals(adminDefaultPassword), "ADMIN_DEFAULT_PASSWORD");
        require(!DEV_AES_KEY.equals(aesEncryptionKey), "AES_ENCRYPTION_KEY");
        require(!DEV_EMAIL_HASH_SECRET.equals(emailHashSecret), "EMAIL_HASH_SECRET");
        if (hmacEnabled) {
            require(!DEV_HMAC_SECRET.equals(hmacSecret), "HMAC_SECRET");
        }

        log.info("SecurityStartupValidator: nenhum segredo de desenvolvimento detectado em produção.");
    }

    private void require(boolean condition, String envVarName) {
        if (!condition) {
            throw new IllegalStateException(
                    "Variavel de ambiente " + envVarName + " nao foi configurada: a aplicacao "
                            + "esta subindo em producao com o valor de fallback de desenvolvimento. "
                            + "Defina " + envVarName + " antes de iniciar em profile 'prod'.");
        }
    }
}
