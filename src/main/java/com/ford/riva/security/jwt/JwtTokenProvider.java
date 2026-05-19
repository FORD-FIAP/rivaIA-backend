package com.ford.riva.security.jwt;

import com.ford.riva.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token.expiration}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenExpirationMs
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret deve ter pelo menos 256 bits (32 bytes) para HS256"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String username, Role role) {
        return buildToken(username, role, TYPE_ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(String username, Role role) {
        return buildToken(username, role, TYPE_REFRESH, refreshTokenExpirationMs);
    }

    private String buildToken(String username, Role role, String type, long expirationMs) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(getTypeFromToken(token));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(getTypeFromToken(token));
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Role getRoleFromToken(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return Role.valueOf(role);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000L;
    }

    private String getTypeFromToken(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
