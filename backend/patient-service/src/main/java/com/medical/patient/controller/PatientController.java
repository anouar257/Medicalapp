package com.medical.patient.controller;

import com.medical.patient.entity.Patient;
import com.medical.patient.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur Patient — gestion du profil (endpoints protégés par JWT).
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Récupère le profil du patient authentifié.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Patient patient) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        // Recharger depuis la BDD pour les données fraîches
        Patient fresh = patientService.findById(patient.getId())
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));
        return ResponseEntity.ok(toProfileMap(fresh));
    }

    /**
     * Met à jour le profil du patient authentifié.
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal Patient patient,
                                              @RequestBody Patient updates) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        try {
            Patient updated = patientService.updateProfile(patient.getId(), updates);
            return ResponseEntity.ok(toProfileMap(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Désactive le compte du patient authentifié.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> deactivateMyAccount(@AuthenticationPrincipal Patient patient) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        patientService.deactivateAccount(patient.getId());
        return ResponseEntity.ok(Map.of("message", "Compte désactivé avec succès"));
    }

    private Map<String, Object> toProfileMap(Patient p) {
        return Map.ofEntries(
                Map.entry("id", p.getId()),
                Map.entry("sexe", p.getSexe().name()),
                Map.entry("prenom", p.getPrenom()),
                Map.entry("nom", p.getNom()),
                Map.entry("dateNaissance", p.getDateNaissance().toString()),
                Map.entry("email", p.getEmail()),
                Map.entry("telephone", p.getTelephone()),
                Map.entry("emailVerifie", p.isEmailVerifie()),
                Map.entry("telephoneVerifie", p.isTelephoneVerifie()),
                Map.entry("cguAcceptees", p.isCguAcceptees()),
                Map.entry("actif", p.isActif()),
                Map.entry("dateInscription", p.getDateInscription().toString())
        );
    }
}
