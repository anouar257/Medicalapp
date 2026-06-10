package com.medical.patient.controller;

import com.medical.patient.entity.Patient;
import com.medical.patient.entity.Proche;
import com.medical.patient.service.ProcheService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur Proche — CRUD complet (endpoints protégés par JWT).
 */
@RestController
@RequestMapping("/api/proches")
public class ProcheController {

    private static final String ERROR_KEY = "error";
    private static final String NON_AUTHENTIFIE = "Non authentifié";
    private static final String MESSAGE_KEY = "message";

    private final ProcheService procheService;

    public ProcheController(ProcheService procheService) {
        this.procheService = procheService;
    }

    /**
     * Liste tous les proches du patient authentifié.
     */
    @GetMapping
    public ResponseEntity<Object> getMyProches(@AuthenticationPrincipal Patient patient) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, NON_AUTHENTIFIE));
        }
        List<Proche> proches = procheService.findByPatientId(patient.getId());
        return ResponseEntity.ok(proches);
    }

    /**
     * Récupère un proche par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getProche(@AuthenticationPrincipal Patient patient,
                                        @PathVariable Long id) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, NON_AUTHENTIFIE));
        }
        try {
            Proche proche = procheService.findByIdAndPatientId(id, patient.getId());
            return ResponseEntity.ok(proche);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Crée un nouveau proche pour le patient authentifié.
     */
    @PostMapping
    public ResponseEntity<Object> createProche(@AuthenticationPrincipal Patient patient,
                                           @Valid @RequestBody Proche proche) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, NON_AUTHENTIFIE));
        }
        try {
            Proche created = procheService.create(patient.getId(), proche);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Met à jour un proche existant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateProche(@AuthenticationPrincipal Patient patient,
                                           @PathVariable Long id,
                                           @Valid @RequestBody Proche updates) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, NON_AUTHENTIFIE));
        }
        try {
            Proche updated = procheService.update(id, patient.getId(), updates);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Supprime un proche.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProche(@AuthenticationPrincipal Patient patient,
                                           @PathVariable Long id) {
        if (patient == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, NON_AUTHENTIFIE));
        }
        try {
            procheService.delete(id, patient.getId());
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Proche supprimé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
}
