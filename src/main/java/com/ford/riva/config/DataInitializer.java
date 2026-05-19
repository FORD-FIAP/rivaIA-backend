package com.ford.riva.config;

import com.ford.riva.crypto.EmailHasher;
import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@riva.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailHasher emailHasher;

    @Value("${admin.default.password}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
            return;
        }

        User admin = User.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .email(DEFAULT_ADMIN_EMAIL)
                .emailHash(emailHasher.hash(DEFAULT_ADMIN_EMAIL))
                .password(passwordEncoder.encode(defaultAdminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Usuário ADMIN default criado (username='{}'). Altere a senha em produção.", DEFAULT_ADMIN_USERNAME);
    }
}
