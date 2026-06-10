package com.medical.patient.controller;

import com.medical.patient.entity.Patient;
import com.medical.patient.entity.Proche;
import com.medical.patient.repository.PatientRepository;
import com.medical.patient.repository.ProcheRepository;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Autorisations pour la messagerie (le patient connecté peut parler en son nom ou pour un
 * proche qu'il gère) — appelé par messaging-service avec secret partagé.
 */
@RestController
@RequestMapping("/api/internal/messages")
public class InternalMessagingAuthorizationController {

  private final ProcheRepository procheRepository;
  private final PatientRepository patientRepository;

  @Value("${integration.messaging-secret:}")
  private String messagingSecret;

  public InternalMessagingAuthorizationController(
      ProcheRepository procheRepository, PatientRepository patientRepository) {
    this.procheRepository = procheRepository;
    this.patientRepository = patientRepository;
  }

  /**
   * Indique si le titulaire du compte patient ({@code ownerPatientId}) peut envoyer un message
   * concernant {@code concernedPersonId} (lui-même ou un de ses proches).
   */
  @GetMapping("/can-represent")
  public Map<String, Boolean> canRepresent(
      @RequestParam("ownerPatientId") Long ownerPatientId,
      @RequestParam("concernedPersonId") Long concernedPersonId,
      @RequestHeader(value = "X-Messaging-Secret", required = false) String secret) {
    requireSecret(secret);
    if (ownerPatientId == null || concernedPersonId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerPatientId et concernedPersonId requis");
    }
    if (ownerPatientId.equals(concernedPersonId)) {
      return Map.of("allowed", true);
    }
    boolean allowed = procheRepository.existsByIdAndPatientId(concernedPersonId, ownerPatientId);
    return Map.of("allowed", allowed);
  }

  @PostMapping("/person-display-names")
  public Map<Long, String> personDisplayNames(
      @RequestBody(required = false) List<Long> ids,
      @RequestHeader(value = "X-Messaging-Secret", required = false) String secret) {
    requireSecret(secret);
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    Set<Long> uniq = new LinkedHashSet<>(ids);
    Map<Long, String> out = new HashMap<>();
    for (Long id : uniq) {
      if (id != null) {
        patientRepository
            .findById(id)
            .map(InternalMessagingAuthorizationController::formatPatientName)
            .filter(s -> !s.isBlank())
            .ifPresent(name -> out.put(id, name));
        if (!out.containsKey(id)) {
          procheRepository
              .findById(id)
              .map(InternalMessagingAuthorizationController::formatProcheName)
              .filter(s -> !s.isBlank())
              .ifPresent(name -> out.put(id, name));
        }
      }
    }
    return out;
  }

  private static String formatPatientName(Patient p) {
    return joinNames(p.getPrenom(), p.getNom());
  }

  private static String formatProcheName(Proche p) {
    return joinNames(p.getPrenom(), p.getNom());
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

  private void requireSecret(String secret) {
    if (messagingSecret == null || messagingSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "integration.messaging-secret non configuré");
    }
    if (secret == null || !messagingSecret.equals(secret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret inter-service invalide");
    }
  }
}
