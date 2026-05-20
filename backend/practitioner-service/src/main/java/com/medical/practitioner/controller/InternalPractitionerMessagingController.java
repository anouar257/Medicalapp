package com.medical.practitioner.controller;

import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/messaging")
public class InternalPractitionerMessagingController {

  private final PractitionerProfileRepository practitionerProfileRepository;

  @Value("${integration.messaging-secret:}")
  private String messagingSecret;

  public InternalPractitionerMessagingController(
      PractitionerProfileRepository practitionerProfileRepository) {
    this.practitionerProfileRepository = practitionerProfileRepository;
  }

  @PostMapping("/practitioner-display-names")
  public Map<Long, String> practitionerDisplayNames(
      @RequestBody(required = false) List<Long> ids,
      @RequestHeader(value = "X-Messaging-Secret", required = false) String secret) {
    requireSecret(secret);
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    Set<Long> uniq = new LinkedHashSet<>();
    for (Long id : ids) {
      if (id != null) {
        uniq.add(id);
      }
    }
    if (uniq.isEmpty()) {
      return Map.of();
    }
    List<PractitionerProfile> profiles = practitionerProfileRepository.findAllByIdInWithProUser(uniq);
    Map<Long, String> out = new HashMap<>();
    for (PractitionerProfile p : profiles) {
      ProUser u = p.getProUser();
      if (u == null) {
        continue;
      }
      String name = joinNames(u.getPrenom(), u.getNom());
      if (!name.isBlank()) {
        out.put(Objects.requireNonNull(p.getId()), name);
      }
    }
    return out;
  }

  private void requireSecret(String secret) {
    if (messagingSecret == null || messagingSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "integration.messaging-secret non configuré");
    }
    if (secret == null || !messagingSecret.equals(secret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret inter-service invalide");
    }
  }

  private static String joinNames(String prenom, String nom) {
    String a = prenom == null ? "" : prenom.trim();
    String b = nom == null ? "" : nom.trim();
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    return a + " " + b;
  }
}
