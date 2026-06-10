package com.medical.agenda.controller;

import com.medical.agenda.dto.AppointmentTypeDTO;
import com.medical.agenda.dto.AppointmentTypePublicDTO;
import com.medical.agenda.service.AppointmentTypeService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/appointment-types")
public class AppointmentTypeController {

  private final AppointmentTypeService service;

  @Value("${integration.agenda-sync-secret:}")
  private String agendaSyncSecret;

  public AppointmentTypeController(AppointmentTypeService service) {
    this.service = service;
  }

  /** Types de visite actifs â€” utilisÃ©s par le parcours patient (durÃ©e par dÃ©faut, code). */
  @GetMapping
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public List<AppointmentTypePublicDTO> listActive() {
    return service.findAll().stream()
        .filter(AppointmentTypeDTO::isActive)
        .map(AppointmentTypeController::toPublic)
        .toList();
  }

  @PostMapping("/sync")
  public AppointmentTypePublicDTO sync(
      @RequestBody AppointmentTypeDTO body,
      @RequestHeader(value = "X-Agenda-Sync-Secret", required = false) String secret) {
    requireAgendaSyncSecret(secret);
    return toPublic(service.syncFromAct(body));
  }

  @DeleteMapping("/sync/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(
      @PathVariable String code,
      @RequestHeader(value = "X-Agenda-Sync-Secret", required = false) String secret) {
    requireAgendaSyncSecret(secret);
    service.deactivateByCode(code);
  }

  private static AppointmentTypePublicDTO toPublic(AppointmentTypeDTO t) {
    AppointmentTypePublicDTO d = new AppointmentTypePublicDTO();
    d.setId(t.getId());
    d.setCode(t.getCode());
    d.setLabel(t.getLabel());
    d.setColorCode(t.getColorCode());
    d.setDefaultDurationMinutes(t.getDefaultDurationMinutes());
    d.setDisplayOrder(t.getDisplayOrder());
    d.setActive(t.isActive());
    d.setPrice(t.getPrice());
    d.setPriceVariable(t.isPriceVariable());
    d.setSourcePractitionerId(t.getSourcePractitionerId());
    d.setSourceActId(t.getSourceActId());
    return d;
  }

  private void requireAgendaSyncSecret(String secret) {
    if (agendaSyncSecret == null || agendaSyncSecret.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "integration.agenda-sync-secret non configuré");
    }
    if (secret == null || !agendaSyncSecret.equals(secret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret de synchronisation agenda invalide");
    }
  }
}
