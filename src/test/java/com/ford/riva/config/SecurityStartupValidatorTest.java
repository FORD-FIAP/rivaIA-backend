package com.ford.riva.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityStartupValidatorTest {

    private static final String DEV_JWT_SECRET = "defaultDevSecretThatIsAtLeast256BitsLongForHS256Algorithm!!";
    private static final String DEV_ADMIN_PASSWORD = "Admin@123456";
    private static final String DEV_AES_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String DEV_EMAIL_HASH_SECRET = "devEmailHashSecretAtLeast32CharsLong!";
    private static final String DEV_HMAC_SECRET = "devHmacSecretAtLeast32CharsLongForDev!";

    private SecurityStartupValidator validatorWithProfile(String... profiles) {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(profiles);
        SecurityStartupValidator validator = new SecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "prodSecretRealmenteForteEUnico1234567890");
        ReflectionTestUtils.setField(validator, "adminDefaultPassword", "SenhaForteReal!2026");
        ReflectionTestUtils.setField(validator, "aesEncryptionKey", "d29ya2luZ0FFU0tleTMyQnl0ZXNMb25nQmFzZTY0IQ==");
        ReflectionTestUtils.setField(validator, "emailHashSecret", "segredoRealDeProducaoComMaisDe32Caracteres");
        ReflectionTestUtils.setField(validator, "hmacEnabled", false);
        ReflectionTestUtils.setField(validator, "hmacSecret", "");
        return validator;
    }

    @Nested
    @DisplayName("fora do profile prod")
    class ForaDeProd {

        @Test
        @DisplayName("nao valida nada em dev, mesmo com segredos default")
        void naoValidaEmDev() {
            SecurityStartupValidator validator = validatorWithProfile("dev");
            ReflectionTestUtils.setField(validator, "jwtSecret", DEV_JWT_SECRET);

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("no profile prod")
    class EmProd {

        @Test
        @DisplayName("sobe normalmente quando todos os segredos foram customizados")
        void sobeComSegredosCustomizados() {
            SecurityStartupValidator validator = validatorWithProfile("prod");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("falha ao subir se JWT_SECRET ainda for o default de dev")
        void falhaComJwtSecretDefault() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "jwtSecret", DEV_JWT_SECRET);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("falha ao subir se ADMIN_DEFAULT_PASSWORD ainda for o default de dev")
        void falhaComAdminPasswordDefault() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "adminDefaultPassword", DEV_ADMIN_PASSWORD);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ADMIN_DEFAULT_PASSWORD");
        }

        @Test
        @DisplayName("falha ao subir se AES_ENCRYPTION_KEY ainda for o default de dev")
        void falhaComAesKeyDefault() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "aesEncryptionKey", DEV_AES_KEY);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AES_ENCRYPTION_KEY");
        }

        @Test
        @DisplayName("falha ao subir se EMAIL_HASH_SECRET ainda for o default de dev")
        void falhaComEmailHashSecretDefault() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "emailHashSecret", DEV_EMAIL_HASH_SECRET);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("EMAIL_HASH_SECRET");
        }

        @Test
        @DisplayName("ignora HMAC_SECRET default quando o HMAC esta desabilitado")
        void ignoraHmacSecretDefaultQuandoDesabilitado() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "hmacEnabled", false);
            ReflectionTestUtils.setField(validator, "hmacSecret", DEV_HMAC_SECRET);

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("falha ao subir se HMAC estiver habilitado com o secret default de dev")
        void falhaComHmacSecretDefaultQuandoHabilitado() {
            SecurityStartupValidator validator = validatorWithProfile("prod");
            ReflectionTestUtils.setField(validator, "hmacEnabled", true);
            ReflectionTestUtils.setField(validator, "hmacSecret", DEV_HMAC_SECRET);

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HMAC_SECRET");
        }
    }
}
