package com.medical.practitioner.controller;

import com.medical.practitioner.dto.BookingWizardOptionsDTO;
import com.medical.practitioner.dto.CabinetHoraireDTO;
import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.dto.PractitionerActDTO;
import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.dto.PractitionerSearchResultDTO;
import com.medical.practitioner.service.BookingWizardService;
import com.medical.practitioner.service.CabinetService;
import com.medical.practitioner.service.PractitionerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API publiques (sans JWT) : questionnaire RDV, lieux affichables sur la carte pour un praticien
 * publié ({@code disponible = true}).
 */
@RestController
@RequestMapping("/api/pro/public")
public class PublicProController {

  private final BookingWizardService bookingWizardService;
  private final PractitionerService practitionerService;
  private final CabinetService cabinetService;

  public PublicProController(
      BookingWizardService bookingWizardService,
      PractitionerService practitionerService,
      CabinetService cabinetService) {
    this.bookingWizardService = bookingWizardService;
    this.practitionerService = practitionerService;
    this.cabinetService = cabinetService;
  }

  @GetMapping("/booking/wizard-options")
  public BookingWizardOptionsDTO wizardOptions(
      @RequestParam(name = "specialtyCode", defaultValue = "DEFAULT") String specialtyCode) {
    return bookingWizardService.optionsForSpecialtyCode(specialtyCode);
  }

  @GetMapping("/practitioners/{id}/locations")
  public List<ConsultationLocationDTO> publicLocations(@PathVariable Long id) {
    return practitionerService.listPublicLocations(id);
  }

  @GetMapping("/practitioners/{id}/acts")
  public List<PractitionerActDTO> publicActs(@PathVariable Long id) {
    return practitionerService.listPublicActs(id);
  }

  @GetMapping("/practitioners/search")
  public List<PractitionerSearchResultDTO> search(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String ville,
      @RequestParam(required = false) String specialty) {
    return practitionerService.searchPublic(name, ville, specialty);
  }

  @GetMapping("/cities")
  public List<String> publicCities() {
    return practitionerService.listPublicCities();
  }

  @GetMapping("/practitioners/featured")
  public List<PractitionerSearchResultDTO> featured(
      @RequestParam(name = "limit", defaultValue = "3") int limit) {
    return practitionerService.featuredPublic(limit);
  }

  @GetMapping("/cabinets/{orgId}/horaires")
  public List<CabinetHoraireDTO> publicCabinetHoraires(@PathVariable Long orgId) {
    List<PractitionerProfileDTO> practitioners = practitionerService.findByOrganization(orgId);
    List<CabinetHoraireDTO> aggregated = new java.util.ArrayList<>();
    
    for (PractitionerProfileDTO p : practitioners) {
      try {
        List<ConsultationLocationDTO> locations = practitionerService.listPublicLocations(p.getId());
        for (ConsultationLocationDTO loc : locations) {
          if (loc.getHoraires() != null) {
            for (ConsultationLocationDTO.HoraireDTO h : loc.getHoraires()) {
              CabinetHoraireDTO dto = new CabinetHoraireDTO();
              dto.setJour(h.getJour());
              dto.setHeureDebut(h.getHeureDebut());
              dto.setHeureFin(h.getHeureFin());
              dto.setContinu(h.isContinu());
              aggregated.add(dto);
            }
          }
        }
      } catch (Exception e) {
        // Ignorer si le profil n'est pas publié / indisponible
      }
    }
    
    if (aggregated.isEmpty()) {
      return cabinetService.findById(orgId).getHoraires();
    }
    return aggregated;
  }

  @GetMapping("/practitioners/{id}/profile")
  public com.medical.practitioner.dto.PractitionerProfileDTO publicProfile(@PathVariable Long id) {
    return practitionerService.getPublicProfile(id);
  }
}
