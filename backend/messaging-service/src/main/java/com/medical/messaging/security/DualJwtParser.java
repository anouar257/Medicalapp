package com.medical.messaging.security;

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
public class DualJwtParser {

  private final SecretKey patientKey;
  private final SecretKey proKey;

  public DualJwtParser(
      @Value("${jwt.patient-secret}") String patientSecret,
      @Value("${jwt.pro-secret}") String proSecret) {
    this.patientKey = Keys.hmacShaKeyFor(patientSecret.getBytes(StandardCharsets.UTF_8));
    this.proKey = Keys.hmacShaKeyFor(proSecret.getBytes(StandardCharsets.UTF_8));
  }

  public Optional<MessagingPrincipal> parse(String bearerTokenWithoutPrefix) {
    Optional<MessagingPrincipal> patient = tryPatient(bearerTokenWithoutPrefix);
    if (patient.isPresent()) {
      return patient;
    }
    return tryPro(bearerTokenWithoutPrefix);
  }

  public boolean isExpired(String bearerTokenWithoutPrefix) {
    return parseClaimsSilent(patientKey, bearerTokenWithoutPrefix)
        .or(() -> parseClaimsSilent(proKey, bearerTokenWithoutPrefix))
        .map(c -> c.getExpiration().before(new Date()))
        .orElse(true);
  }

  private Optional<MessagingPrincipal> tryPatient(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(patientKey).build().parseSignedClaims(token).getPayload();
      Long patientId = c.get("patientId", Long.class);
      if (patientId == null) {
        return Optional.empty();
      }
      if (!Boolean.TRUE.equals(c.get("isVerified", Boolean.class))) {
        return Optional.empty();
      }
      return Optional.of(new MessagingPrincipal.MessagingPatient(patientId));
    } catch (Exception ignored) {
    }
    return Optional.empty();
  }

  private Optional<MessagingPrincipal> tryPro(String token) {
    try {
      Claims c = Jwts.parser().verifyWith(proKey).build().parseSignedClaims(token).getPayload();
      Long userId = c.get("userId", Long.class);
      if (userId == null) {
        return Optional.empty();
      }
      if (!Boolean.TRUE.equals(c.get("isVerified", Boolean.class))) {
        return Optional.empty();
      }
      Long practitionerProfileId = c.get("practitionerProfileId", Long.class);
      String role = c.get("role", String.class);
      return Optional.of(
          new MessagingPrincipal.MessagingPractitioner(
              userId, practitionerProfileId, role != null ? role : ""));
    } catch (Exception ignored) {
    }
    return Optional.empty();
  }

  private static Optional<Claims> parseClaimsSilent(SecretKey key, String token) {
    try {
      return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
