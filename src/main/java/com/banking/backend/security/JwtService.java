package com.banking.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String customerNo, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(customerNo)
                .claim("role", role)
                .claim("type", TYPE_ACCESS)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String customerNo) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(customerNo)
                .claim("type", TYPE_REFRESH)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String extractCustomerNo(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractType(String token) {
        String type = parseClaims(token).get("type", String.class);
        return type == null ? TYPE_ACCESS : type;
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractType(token));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractType(token));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String customerNo = extractCustomerNo(token);
        return customerNo.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
