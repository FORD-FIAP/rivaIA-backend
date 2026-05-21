package com.ford.riva.service;

import com.ford.riva.crypto.EmailHasher;
import com.ford.riva.dto.auth.LoginRequest;
import com.ford.riva.dto.auth.RefreshTokenRequest;
import com.ford.riva.dto.auth.RegisterRequest;
import com.ford.riva.dto.auth.TokenResponse;
import com.ford.riva.model.AuditAction;
import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import com.ford.riva.security.filter.MdcFilter;
import com.ford.riva.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String LOGIN_RESOURCE = "/api/v1/auth/login";
    private static final String REGISTER_RESOURCE = "/api/v1/auth/register";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailHasher emailHasher;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username já está em uso");
        }
        String emailHash = emailHasher.hash(request.getEmail());
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .emailHash(emailHash)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
        MDC.put(MdcFilter.USER_ID, user.getUsername());
        log.info("Usuário registrado: {}", user.getUsername());
        auditService.log(AuditAction.USER_CREATED, REGISTER_RESOURCE, "username=" + user.getUsername());

        return buildTokens(user.getUsername(), user.getRole());
    }

    public TokenResponse login(LoginRequest request) {
        String clientIp = MDC.get(MdcFilter.CLIENT_IP);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            handleFailedLogin(request.getUsername(), clientIp, ex);
            throw ex;
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        loginAttemptService.reset(clientIp);
        MDC.put(MdcFilter.USER_ID, user.getUsername());
        log.info("Login realizado: {}", user.getUsername());
        auditService.log(AuditAction.LOGIN, LOGIN_RESOURCE, "username=" + user.getUsername());

        return buildTokens(user.getUsername(), user.getRole());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (!user.isEnabled()) {
            throw new DisabledException("Conta desabilitada");
        }

        return buildTokens(user.getUsername(), user.getRole());
    }

    private void handleFailedLogin(String username, String clientIp, AuthenticationException ex) {
        int failures = loginAttemptService.recordFailure(clientIp);
        log.warn("Falha de login para username='{}' (IP={}, motivo={})",
                username, clientIp, ex.getClass().getSimpleName());

        if (failures >= loginAttemptService.getSuspiciousThreshold()) {
            log.error("Possível brute force detectado: IP={} com {} falhas de login em 5 minutos",
                    clientIp, failures);
        }
        auditService.log(AuditAction.LOGIN_FAILED, LOGIN_RESOURCE, "username=" + username);
    }

    private TokenResponse buildTokens(String username, Role role) {
        String access = jwtTokenProvider.generateAccessToken(username, role);
        String refresh = jwtTokenProvider.generateRefreshToken(username, role);
        return TokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }
}
