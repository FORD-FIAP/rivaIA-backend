package com.ford.riva.service;

import com.ford.riva.crypto.EmailHasher;
import com.ford.riva.model.AuditAction;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnonymizationService {

    private static final String DELETED_USERNAME_PREFIX = "deleted_user_";
    private static final String DELETED_EMAIL_DOMAIN = "@anonymized.local";
    private static final String DELETED_PASSWORD = "{DELETED}";

    private final UserRepository userRepository;
    private final EmailHasher emailHasher;
    private final AuditService auditService;

    @Transactional
    public void anonymizeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String anonymizedEmail = DELETED_USERNAME_PREFIX + userId + DELETED_EMAIL_DOMAIN;

        user.setUsername(DELETED_USERNAME_PREFIX + userId);
        user.setEmail(anonymizedEmail);
        user.setEmailHash(emailHasher.hash(anonymizedEmail));
        user.setPassword(DELETED_PASSWORD);
        user.setEnabled(false);

        userRepository.save(user);
        log.info("Usuário {} anonimizado (LGPD)", userId);
        auditService.log(AuditAction.USER_DELETED, "/api/v1/users/" + userId,
                "Usuário anonimizado conforme LGPD");
    }
}
