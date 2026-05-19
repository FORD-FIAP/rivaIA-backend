package com.ford.riva.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SafeTextValidatorTest {

    private SafeTextValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new SafeTextValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    @DisplayName("null é considerado válido (use @NotNull/@NotBlank pra isso)")
    void nullIsValid() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("string legítima é válida")
    void legitStringIsValid() {
        assertThat(validator.isValid("Ford Ranger Raptor", context)).isTrue();
    }

    @Test
    @DisplayName("string com XSS é inválida")
    void xssIsInvalid() {
        assertThat(validator.isValid("<script>alert(1)</script>", context)).isFalse();
    }

    @Test
    @DisplayName("string com SQL injection é inválida")
    void sqlInjectionIsInvalid() {
        assertThat(validator.isValid("' UNION SELECT * FROM users--", context)).isFalse();
    }

    @Test
    @DisplayName("string com command injection é inválida")
    void commandInjectionIsInvalid() {
        assertThat(validator.isValid("$(whoami)", context)).isFalse();
    }

    @Test
    @DisplayName("string com path traversal é inválida")
    void pathTraversalIsInvalid() {
        assertThat(validator.isValid("../../etc/passwd", context)).isFalse();
    }
}
