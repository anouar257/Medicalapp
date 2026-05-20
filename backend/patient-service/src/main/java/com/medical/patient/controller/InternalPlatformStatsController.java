package com.medical.patient.controller;

import com.medical.patient.dto.PatientDTO;
import com.medical.patient.entity.Patient;
import com.medical.patient.repository.PatientRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Compteurs internes pour agrégation plateforme (practitioner-service / super-admin).
 * Protégé par secret partagé {@code X-Platform-Stats-Key}, pas par JWT patient.
 */
@RestController
@RequestMapping("/api/internal/stats")
public class InternalPlatformStatsController {

  private final PatientRepository patientRepository;

  @Value("${app.platform-stats.secret}")
  private String platformStatsSecret;

  public InternalPlatformStatsController(PatientRepository patientRepository) {
    this.patientRepository = patientRepository;
  }

  @GetMapping("/patient-count")
  public Map<String, Long> patientCount(@RequestHeader("X-Platform-Stats-Key") String key) {
    checkKey(key);
    return Map.of("totalPatients", patientRepository.count());
  }

  @GetMapping("/patients")
  public List<PatientDTO> listPatients(@RequestHeader("X-Platform-Stats-Key") String key) {
    checkKey(key);
    return patientRepository.findAll().stream()
        .map(this::mapToDTO)
        .toList();
  }

  @PutMapping("/patients/{id}/toggle-active")
  public void togglePatientActive(
      @RequestHeader("X-Platform-Stats-Key") String key, @PathVariable Long id) {
    checkKey(key);
    Patient p = patientRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
    p.setActif(!p.isActif());
    patientRepository.save(p);
  }

  private void checkKey(String key) {
    if (key == null || !key.equals(platformStatsSecret)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clé plateforme invalide");
    }
  }

  private PatientDTO mapToDTO(Patient p) {
    PatientDTO dto = new PatientDTO();
    dto.setId(p.getId());
    dto.setPrenom(p.getPrenom());
    dto.setNom(p.getNom());
    dto.setEmail(p.getEmail());
    dto.setTelephone(p.getTelephone());
    dto.setActif(p.isActif());
    dto.setDateInscription(p.getDateInscription());
    return dto;
  }
}
