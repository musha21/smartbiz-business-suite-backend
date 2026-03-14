package com.example.smartBiz.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expirationMs}")
    private long expirationMs;

    // 15 minutes in ms
    private final long ACCESS_TOKEN_EXPIRATION = 900_000L;
    // 7 days in ms
    private final long REFRESH_TOKEN_EXPIRATION = 604_800_000L;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, Long businessId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("businessId", businessId) // can be null for ADMIN
                .claim("role", role)             // OWNER / ADMIN
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        Object v = getAllClaims(token).get("userId");
        return v == null ? null : ((Number) v).longValue();
    }

    public Long getBusinessId(String token) {
        Object v = getAllClaims(token).get("businessId");
        return v == null ? null : ((Number) v).longValue();
    }

    public String getRole(String token) {
        Object v = getAllClaims(token).get("role");
        return v == null ? null : v.toString();
    }
}
