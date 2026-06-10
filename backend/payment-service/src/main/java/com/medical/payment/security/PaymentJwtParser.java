package com.medical.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class PaymentJwtParser {

  private final SecretKey proKey;

  public PaymentJwtParser(@Value("${jwt.pro-secret}") String proSecret) {
    this.proKey = Keys.hmacShaKeyFor(proSecret.getBytes(StandardCharsets.UTF_8));
  }

  public Authentication parseToken(String token) {
    try {
      Claims claims = Jwts.parser().verifyWith(proKey).build().parseSignedClaims(token).getPayload();
      Long orgId = null;
      if (claims.get("organizationId") != null) {
        orgId = ((Number) claims.get("organizationId")).longValue();
      }
      String role = claims.get("role", String.class);
      String email = claims.getSubject();

      PaymentProPrincipal principal = new PaymentProPrincipal(orgId, role, email);
      return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    } catch (Exception e) {
      return null;
    }
  }
}
