package com.medical.agenda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgendaDualJwtParser {

  private final SecretKey patientKey;
  private final SecretKey proKey;

  public AgendaDualJwtParser(
      @Value("${jwt.patient-secret}") String patientSecret,
      @Value("${jwt.pro-secret}") String proSecret) {
    this.patientKey = Keys.hmacShaKeyFor(patientSecret.getBytes(StandardCharsets.UTF_8));
    this.proKey = Keys.hmacShaKeyFor(proSecret.getBytes(StandardCharsets.UTF_8));
  }

  public Optional<Object> parsePrincipal(String bearerWithoutPrefix) {
    Optional<Object> patient = tryPatient(bearerWithoutPrefix);
    if (patient.isPresent()) {
      return patient;
    }
    return tryPro(bearerWithoutPrefix);
  }

  public boolean isExpired(String bearerWithoutPrefix) {
    return parseClaimsSilent(patientKey, bearerWithoutPrefix)
        .or(() -> parseClaimsSilent(proKey, bearerWithoutPrefix))
        .map(c -> c.getExpiration().before(new Date()))
        .orElse(true);
  }

  private static boolean isVerifiedClaim(Claims c) {
    Boolean v = c.get("isVerified", Boolean.class);
    return Boolean.TRUE.equals(v);
  }

  private Optional<Object> tryPatient(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(patientKey).build().parseSignedClaims(token).getPayload();
      Long patientId = c.get("patientId", Long.class);
      if (patientId == null) {
        return Optional.empty();
      }
      if (!isVerifiedClaim(c)) {
        return Optional.empty();
      }
      return Optional.of(new AgendaPatientPrincipal(patientId));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private Optional<Object> tryPro(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(proKey).build().parseSignedClaims(token).getPayload();
      Long userId = c.get("userId", Long.class);
      if (userId == null) {
        return Optional.empty();
      }
      if (!isVerifiedClaim(c)) {
        return Optional.empty();
      }
      String role = c.get("role", String.class);
      if (role == null || role.isBlank()) {
        return Optional.empty();
      }
      Long organizationId = c.get("organizationId", Long.class);
      return Optional.of(
          new AgendaProPrincipal(
              userId, c.getSubject(), role, organizationId));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private static Optional<Claims> parseClaimsSilent(SecretKey key, String token) {
    try {
      return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
