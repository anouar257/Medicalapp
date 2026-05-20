package com.medical.practitioner.config;

import com.medical.practitioner.entity.ProUserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire JWT pour les utilisateurs professionnels (cabinet).
 *
 * <p>Le token contient :
 * <ul>
 *   <li>le subject = email du ProUser ;</li>
 *   <li>le claim {@code userId} = identifiant interne du ProUser ;</li>
 *   <li>le claim {@code role} = rôle exact ({@code ADMIN} plateforme, {@code PRATICIEN}, {@code ASSISTANT}) ;</li>
 *   <li>le claim {@code organizationId} = identifiant du cabinet (null pour l'administration plateforme) ;</li>
 *   <li>le claim optionnel {@code practitionerProfileId} = profil praticien (inter-services / messagerie) ;</li>
 *   <li>le claim {@code isVerified} = email vérifié par OTP (requis par agenda-service et messaging-service).</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, Long userId, ProUserRole role, Long organizationId) {
        return generateToken(email, userId, role, organizationId, null, false);
    }

    public String generateToken(
            String email,
            Long userId,
            ProUserRole role,
            Long organizationId,
            Long practitionerProfileId,
            boolean emailVerified) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder =
                Jwts.builder()
                        .subject(email)
                        .claim("userId", userId)
                        .claim("role", role.name())
                        .claim("organizationId", organizationId)
                        .claim("isVerified", emailVerified)
                        .issuedAt(now)
                        .expiration(expiry);
        if (practitionerProfileId != null) {
            builder.claim("practitionerProfileId", practitionerProfileId);
        }
        return builder.signWith(key).compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long extractOrganizationId(String token) {
        return parseClaims(token).get("organizationId", Long.class);
    }

    /** Profil praticien ({@code PractitionerProfile.id}), absent pour les comptes sans profil. */
    public Long extractPractitionerProfileId(String token) {
        return parseClaims(token).get("practitionerProfileId", Long.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
