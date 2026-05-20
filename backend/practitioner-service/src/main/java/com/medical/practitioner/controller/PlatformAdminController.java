package com.medical.practitioner.controller;

import com.medical.practitioner.dto.MedicalOrganizationDTO;
import com.medical.practitioner.dto.PlatformSummaryDTO;
import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.dto.ProUserDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.security.AccessPolicies;
import com.medical.practitioner.service.CabinetService;
import com.medical.practitioner.service.PlatformStatsService;
import com.medical.practitioner.service.PractitionerService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pro/platform")
public class PlatformAdminController {

  private final CabinetService cabinetService;
  private final PractitionerService practitionerService;
  private final PlatformStatsService platformStatsService;

  public PlatformAdminController(
      CabinetService cabinetService,
      PractitionerService practitionerService,
      PlatformStatsService platformStatsService) {
    this.cabinetService = cabinetService;
    this.practitionerService = practitionerService;
    this.platformStatsService = platformStatsService;
  }

  @GetMapping("/stats/platform-summary")
  public PlatformSummaryDTO platformSummary(@AuthenticationPrincipal ProUser user) {
    AccessPolicies.requirePlatformAdmin(user);
    return platformStatsService.buildPlatformSummary();
  }

  @GetMapping("/cabinets")
  public List<MedicalOrganizationDTO> listCabinets(@AuthenticationPrincipal ProUser user) {
    AccessPolicies.requirePlatformAdmin(user);
    return cabinetService.listAllOrganizations();
  }

  @GetMapping("/cabinets/{id}/users")
  public List<ProUserDTO> listCabinetUsers(
      @AuthenticationPrincipal ProUser user, @PathVariable Long id) {
    AccessPolicies.requirePlatformAdmin(user);
    return cabinetService.listUsersByOrganization(id);
  }

  @GetMapping("/practitioners")
  public List<PractitionerProfileDTO> listPractitioners(@AuthenticationPrincipal ProUser user) {
    AccessPolicies.requirePlatformAdmin(user);
    return practitionerService.findAllForPlatformAdmin();
  }

  @GetMapping("/patients")
  public List<Map<String, Object>> listPatients(@AuthenticationPrincipal ProUser user) {
    AccessPolicies.requirePlatformAdmin(user);
    return platformStatsService.listPlatformPatients();
  }

  @PutMapping("/patients/{id}/toggle-active")
  public void togglePatientActive(
      @AuthenticationPrincipal ProUser user, @PathVariable Long id) {
    AccessPolicies.requirePlatformAdmin(user);
    platformStatsService.togglePatientActive(id);
  }

  @PutMapping("/cabinets/{id}")
  public MedicalOrganizationDTO updateCabinet(
      @AuthenticationPrincipal ProUser user,
      @PathVariable Long id,
      @RequestBody MedicalOrganizationDTO body) {
    AccessPolicies.requirePlatformAdmin(user);
    return cabinetService.updateForActor(user, id, body);
  }

  @PutMapping("/users/{userId}/toggle-active")
  public void toggleUserActive(@AuthenticationPrincipal ProUser user, @PathVariable Long userId) {
    AccessPolicies.requirePlatformAdmin(user);
    // On récupère l'état actuel pour savoir s'il faut activer ou désactiver
    cabinetService.toggleUserStatus(user, userId);
  }
}
