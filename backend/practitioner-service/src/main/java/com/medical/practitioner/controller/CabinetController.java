package com.medical.practitioner.controller;

import com.medical.practitioner.dto.CreateProUserRequest;
import com.medical.practitioner.dto.MedicalOrganizationDTO;
import com.medical.practitioner.dto.ProUserDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.service.CabinetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pro/cabinets")
public class CabinetController {

  private final CabinetService cabinetService;

  public CabinetController(CabinetService cabinetService) {
    this.cabinetService = cabinetService;
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN','ASSISTANT')")
  public MedicalOrganizationDTO getOne(
      @AuthenticationPrincipal ProUser user, @PathVariable Long id) {
    return cabinetService.findByIdForActor(user, id);
  }

  @GetMapping("/me")
  public ResponseEntity<MedicalOrganizationDTO> getMyCabinet(@AuthenticationPrincipal ProUser user) {
    if (user.getOrganization() == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(cabinetService.findById(user.getOrganization().getId()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN')")
  public MedicalOrganizationDTO update(
      @AuthenticationPrincipal ProUser user,
      @PathVariable Long id,
      @RequestBody MedicalOrganizationDTO body) {
    return cabinetService.updateForActor(user, id, body);
  }

  @GetMapping("/{id}/users")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN')")
  public List<ProUserDTO> listUsers(@AuthenticationPrincipal ProUser user, @PathVariable Long id) {
    return cabinetService.listUsersForActor(user, id);
  }

  /** Création d'un compte assistant au sein d'un cabinet. */
  @PostMapping("/{id}/users")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN')")
  public ResponseEntity<ProUserDTO> createUser(
      @AuthenticationPrincipal ProUser user,
      @PathVariable Long id,
      @Valid @RequestBody CreateProUserRequest req) {
    ProUserDTO created = cabinetService.createUserInCabinet(user, id, req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @DeleteMapping("/users/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN')")
  public ResponseEntity<Void> desactiver(
      @AuthenticationPrincipal ProUser user, @PathVariable Long userId) {
    cabinetService.desactiverUser(user, userId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/users/{userId}/reactivate")
  @PreAuthorize("hasAnyRole('ADMIN','PRATICIEN')")
  public ResponseEntity<Void> reactiver(
      @AuthenticationPrincipal ProUser user, @PathVariable Long userId) {
    cabinetService.reactiverUser(user, userId);
    return ResponseEntity.noContent().build();
  }
}
