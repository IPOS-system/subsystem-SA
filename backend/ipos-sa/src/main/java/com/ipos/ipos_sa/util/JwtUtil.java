package com.ipos.ipos_sa.util;

import com.ipos.ipos_sa.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for JWT token generation and validation.
 * Uses JJWT 0.12.x API with modern standards.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-change-this-in-production-at-least-32-chars}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")  // 24 hours in milliseconds
    private long jwtExpiration;

    /**
     * Generate a JWT token for the authenticated user.
     *
     * @param userId user ID
     * @param username username
     * @param role user role
     * @return JWT token string
     */
    public String generateToken(Integer userId, String username, User.Role role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract username from JWT token.
     *
     * @param token JWT token string
     * @return username
     */
    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token string
     * @return user ID
     */
    public Integer getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Integer.class);
    }

    /**
     * Extract role from JWT token.
     *
     * @param token JWT token string
     * @return user role as string
     */
    public String getRoleFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    /**
     * Validate JWT token.
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param authHeader Authorization header value (e.g., "Bearer token_here")
     * @return token string, or null if header is invalid
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
