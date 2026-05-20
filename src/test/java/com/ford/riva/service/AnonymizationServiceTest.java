package com.ford.riva.service;

import com.ford.riva.crypto.EmailHasher;
import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private EmailHasher emailHasher;
    private AnonymizationService service;

    @BeforeEach
    void setUp() {
        emailHasher = new EmailHasher("testEmailHashSecretAtLeast32CharsLong!");
        service = new AnonymizationService(userRepository, emailHasher, auditService);
    }

    private User sampleUser(Long id) {
        return User.builder()
                .id(id)
                .username("realname")
                .email("real.email@example.com")
                .emailHash(emailHasher.hash("real.email@example.com"))
                .password("$2a$12$realbcryptedpassword")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("anonymizeUser substitui dados pessoais por placeholders")
    void anonymizesPersonalData() {
        Long userId = 42L;
        User user = sampleUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.anonymizeUser(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("deleted_user_42");
        assertThat(saved.getEmail()).isEqualTo("deleted_user_42@anonymized.local");
        assertThat(saved.getEmailHash()).isEqualTo(emailHasher.hash("deleted_user_42@anonymized.local"));
        assertThat(saved.getPassword()).isEqualTo("{DELETED}");
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("emailHash do usuário anonimizado é diferente do original")
    void emailHashChanges() {
        Long userId = 7L;
        User user = sampleUser(userId);
        String originalHash = user.getEmailHash();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.anonymizeUser(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmailHash()).isNotEqualTo(originalHash);
    }

    @Test
    @DisplayName("lança EntityNotFoundException quando usuário não existe")
    void throwsWhenNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anonymizeUser(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
