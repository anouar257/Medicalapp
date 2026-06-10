package com.medical.agenda.controller;

import com.medical.agenda.repository.AppointmentRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints internes pour le messaging-service (secret partagé, pas de JWT patient/pro).
 */
@RestController
@RequestMapping("/api/internal/messages")
public class InternalMessagingController {

  private final AppointmentRepository appointmentRepository;

  @Value("${integration.messaging-secret:}")
  private String messagingSecret;

  public InternalMessagingController(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  /**
   * Vérifie qu'au moins un RDV (passé ou futur, non annulé) existe entre le patient et le
   * praticien identifié par son {@code PractitionerProfile.id} côté agenda ({@code
   * Doctor.externalPractitionerId}).
   */
  @GetMapping("/has-relationship")
  public Map<String, Boolean> hasRelationship(
      @RequestParam("patientId") Long patientId,
      @RequestParam("externalPractitionerId") Long externalPractitionerId,
      @RequestParam(value = "completedOnly", required = false, defaultValue = "false") boolean completedOnly,
      @RequestHeader(value = "X-Messaging-Secret", required = false) String secret) {
    requireSecret(secret);
    if (patientId == null || externalPractitionerId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId et externalPractitionerId requis");
    }
    boolean exists;
    if (completedOnly) {
      exists = appointmentRepository.countCompletedRelationBetweenPatientAndPractitionerProfile(
          patientId, externalPractitionerId) > 0;
    } else {
      exists = appointmentRepository.countActiveRelationBetweenPatientAndPractitionerProfile(
          patientId, externalPractitionerId) > 0;
    }
    return Map.of("exists", exists);
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
