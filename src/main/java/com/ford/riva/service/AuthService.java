package com.ford.riva.service;

import com.ford.riva.dto.auth.LoginRequest;
import com.ford.riva.dto.auth.RefreshTokenRequest;
import com.ford.riva.dto.auth.RegisterRequest;
import com.ford.riva.dto.auth.TokenResponse;
import com.ford.riva.model.Role;
import com.ford.riva.model.User;
import com.ford.riva.repository.UserRepository;
import com.ford.riva.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username já está em uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Usuário registrado: {}", user.getUsername());

        return buildTokens(user.getUsername(), user.getRole());
    }

    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            log.warn("Falha de login para username='{}': credenciais inválidas", request.getUsername());
            throw ex;
        } catch (DisabledException ex) {
            log.warn("Falha de login para username='{}': conta desabilitada", request.getUsername());
            throw ex;
        } catch (AuthenticationException ex) {
            log.warn("Falha de login para username='{}': {}", request.getUsername(), ex.getClass().getSimpleName());
            throw ex;
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        log.info("Login realizado: {}", user.getUsername());
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
