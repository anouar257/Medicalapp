package com.medical.practitioner.controller;

import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.entity.VerificationStatus;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.security.AccessPolicies;
import com.medical.practitioner.service.PractitionerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pro/practitioners")
public class PractitionerController {

    private static final String KEY_EXISTS = "exists";

    private final PractitionerService practitionerService;
    private final PractitionerProfileRepository practitionerProfileRepository;

    public PractitionerController(
            PractitionerService practitionerService,
            PractitionerProfileRepository practitionerProfileRepository) {
        this.practitionerService = practitionerService;
        this.practitionerProfileRepository = practitionerProfileRepository;
    }

    /** Profil du praticien connecté (token JWT requis). */
    @GetMapping("/me")
    public PractitionerProfileDTO me(@AuthenticationPrincipal ProUser user) {
        if (AccessPolicies.isPlatformAdmin(user)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Pas de profil praticien pour ce compte");
        }
        return practitionerService.findByProUserId(user.getId());
    }

    @GetMapping("/{id}")
    public PractitionerProfileDTO getOne(@PathVariable Long id) {
        return practitionerService.findById(id);
    }

    @GetMapping("/by-organization/{organizationId}")
    public List<PractitionerProfileDTO> listByOrganization(@PathVariable Long organizationId) {
        return practitionerService.findByOrganization(organizationId);
    }



    @PutMapping("/{id}")
    public PractitionerProfileDTO update(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id,
            @RequestBody PractitionerProfileDTO body) {
        AccessPolicies.requireOwnerOrCabinetMember(user, id, practitionerProfileRepository);
        return practitionerService.update(id, body);
    }

    /**
     * Met à jour le statut d'une vérification (identité ou droit-exercer).
     * Les pièces uploadées sont fournies en URL ({@code docUrl}).
     */
    @PutMapping("/{id}/verifications/{type}")
    public ResponseEntity<PractitionerProfileDTO> updateVerification(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id,
            @PathVariable String type,
            @RequestBody Map<String, String> body) {
        if (AccessPolicies.isPlatformAdmin(user)) {
            // Admin can set any status
        } else {
            if (user == null || user.getRole() == ProUserRole.ASSISTANT) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
            }
            AccessPolicies.requireOwnerOrCabinetMember(user, id, practitionerProfileRepository);
            String statusStr = body.get("status");
            if (statusStr != null && !"EN_ATTENTE".equals(statusStr)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'administrateur peut valider ou rejeter un profil");
            }
        }
        VerificationStatus status = VerificationStatus.valueOf(body.getOrDefault("status", "EN_ATTENTE"));
        String docUrl = body.get("docUrl");
        return ResponseEntity.ok(practitionerService.setVerification(id, type, status, docUrl));
    }

    @PutMapping("/me")
    public PractitionerProfileDTO updateMe(@AuthenticationPrincipal ProUser user,
                                           @RequestBody PractitionerProfileDTO body) {
        if (AccessPolicies.isPlatformAdmin(user)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Pas de profil praticien pour ce compte");
        }
        PractitionerProfileDTO me = practitionerService.findByProUserId(user.getId());
        return practitionerService.update(me.getId(), body);
    }

    @GetMapping("/me/exists")
    public ResponseEntity<Map<String, Boolean>> meExists(@AuthenticationPrincipal ProUser user,
                                                         @RequestParam(required = false) Long fallbackId) {
        if (AccessPolicies.isPlatformAdmin(user)) {
            return ResponseEntity.ok(Map.of(KEY_EXISTS, false));
        }
        try {
            practitionerService.findByProUserId(user.getId());
            return ResponseEntity.ok(Map.of(KEY_EXISTS, true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(KEY_EXISTS, false));
        }
    }
}
