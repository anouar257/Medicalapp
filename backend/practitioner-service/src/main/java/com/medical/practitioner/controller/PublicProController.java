package com.medical.practitioner.controller;

import com.medical.practitioner.dto.BookingWizardOptionsDTO;
import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.service.BookingWizardService;
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

  public PublicProController(
      BookingWizardService bookingWizardService, PractitionerService practitionerService) {
    this.bookingWizardService = bookingWizardService;
    this.practitionerService = practitionerService;
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
}
