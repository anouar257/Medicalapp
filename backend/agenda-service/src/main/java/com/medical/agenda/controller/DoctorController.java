package com.medical.agenda.controller;

import com.medical.agenda.dto.DoctorDTO;
import com.medical.agenda.service.DoctorService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

  private final DoctorService doctorService;

  @Value("${integration.agenda-sync-secret:}")
  private String agendaSyncSecret;

  public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public List<DoctorDTO> list() {
    return doctorService.findAll();
  }

  @GetMapping("/by-external/{externalPractitionerId}")
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public DoctorDTO getByExternalPractitionerId(@PathVariable Long externalPractitionerId) {
    return doctorService.findByExternalPractitionerId(externalPractitionerId);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('PRATICIEN','ADMIN')")
  public DoctorDTO create(@RequestBody DoctorDTO body) {
    return doctorService.create(body);
  }

  /** Corps JSON : {@code name} obligatoire, {@code colorCode} optionnel (inchangé si absent ou vide). */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('PRATICIEN','ADMIN')")
  public DoctorDTO update(@PathVariable Long id, @RequestBody DoctorDTO body) {
    return doctorService.update(id, body);
  }


  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('PRATICIEN','ADMIN')")
  public void delete(@PathVariable Long id) {
    doctorService.delete(id);
  }

  /**
   * Endpoint de synchronisation appelé par le practitioner-service.
   *
   * <p>Crée ou met à jour un Doctor à partir d'un identifiant externe ({@code externalPractitionerId})
   * pointant vers le {@code PractitionerProfile} du practitioner-service. Permet d'afficher
   * rapidement le nom, la couleur, la photo et la spécialité dans le calendrier sans appel
   * inter-services à chaque requête d'agenda.
   */
  @PostMapping("/sync")
  public DoctorDTO sync(
      @RequestBody Map<String, Object> body,
      @RequestHeader(value = "X-Agenda-Sync-Secret", required = false) String secret) {
    requireAgendaSyncSecret(secret);
    Object idVal = body.get("externalPractitionerId");
    Long externalId = idVal == null ? null : Long.valueOf(idVal.toString());
    String name = body.get("name") == null ? null : body.get("name").toString();
    String colorCode = body.get("colorCode") == null ? null : body.get("colorCode").toString();
    String photoUrl = body.get("photoUrl") == null ? null : body.get("photoUrl").toString();
    String specialty = body.get("specialty") == null ? null : body.get("specialty").toString();
    String specialtyCode = body.get("specialtyCode") == null ? null : body.get("specialtyCode").toString();
    Long organizationId = null;
    Object orgVal = body.get("organizationId");
    if (orgVal != null) {
      organizationId = Long.valueOf(orgVal.toString());
    }
    return doctorService.syncFromPractitioner(
        externalId, name, colorCode, photoUrl, specialty, specialtyCode, organizationId);
  }

  /** Désynchronise un praticien (suppression du Doctor si aucun rendez-vous n'est lié). */
  @DeleteMapping("/sync/{externalPractitionerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unsync(
      @PathVariable Long externalPractitionerId,
      @RequestHeader(value = "X-Agenda-Sync-Secret", required = false) String secret) {
    requireAgendaSyncSecret(secret);
    doctorService.unsyncByExternalPractitionerId(externalPractitionerId);
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
