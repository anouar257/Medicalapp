package com.medical.practitioner.controller;

import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.service.PractitionerService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/reviews")
public class InternalReviewSyncController {

  private final PractitionerService practitionerService;

  @Value("${agenda.sync-secret:}")
  private String agendaSyncSecret;

  public InternalReviewSyncController(PractitionerService practitionerService) {
    this.practitionerService = practitionerService;
  }

  @PostMapping("/summary")
  public PractitionerProfileDTO syncSummary(
      @RequestBody Map<String, Object> body,
      @RequestHeader(value = "X-Agenda-Sync-Secret", required = false) String secret) {
    requireSecret(secret);
    Object idVal = body.get("externalPractitionerId");
    if (idVal == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalPractitionerId requis");
    }
    Long practitionerId = Long.valueOf(idVal.toString());

    Double globalRating = null;
    Object ratingVal = body.get("globalRating");
    if (ratingVal != null) {
      globalRating = Double.valueOf(ratingVal.toString());
    }

    Integer reviewCount = null;
    Object countVal = body.get("reviewCount");
    if (countVal != null) {
      reviewCount = Integer.valueOf(countVal.toString());
    }

    return practitionerService.updateReviewSummary(practitionerId, globalRating, reviewCount);
  }

  private void requireSecret(String secret) {
    if (agendaSyncSecret == null || agendaSyncSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "agenda.sync-secret non configuré");
    }
    if (secret == null || !agendaSyncSecret.equals(secret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret de synchronisation invalide");
    }
  }
}
