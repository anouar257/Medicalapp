package com.medical.agenda.controller;

import com.medical.agenda.dto.AppointmentDTO;
import com.medical.agenda.dto.AppointmentStatusPatchDTO;
import com.medical.agenda.dto.PatientBookingRequestDTO;
import com.medical.agenda.security.AgendaProPrincipal;
import com.medical.agenda.service.AppointmentService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;

  public AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  /** Paramètres ISO-8601 : {@code start} inclus, {@code end} exclus (fenêtre affichée calendrier). */
  @GetMapping
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public List<AppointmentDTO> list(
      Authentication authentication,
      @RequestParam("start") Instant rangeStart,
      @RequestParam("end") Instant rangeEnd) {
    return appointmentService.findInRange(authentication, rangeStart, rangeEnd);
  }

  @PostMapping("/patient-booking")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('PATIENT')")
  public AppointmentDTO createPatientBooking(
      Authentication authentication, @RequestBody PatientBookingRequestDTO body) {
    return appointmentService.createPatientBooking(authentication, body);
  }

  @GetMapping("/available-slots")
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public com.medical.agenda.dto.AgendaSlotsDTO getAvailableSlots(
      @RequestParam("doctorId") Long doctorId,
      @RequestParam("date") String dateString,
      @RequestParam(value = "durationMinutes", defaultValue = "15") int duration,
      @RequestParam(value = "timezone", required = false) String timezone) {
    return appointmentService.getAvailableSlots(doctorId, dateString, duration, timezone);
  }

  /**
   * Demandes {@code PENDING} pour le cabinet connecté (JWT pro : {@code organizationId}).
   * Réservé aux comptes cabinet — pas d’agrégat « tous les cabinets ».
   */
  @GetMapping("/cabinet/pending")
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN')")
  public List<AppointmentDTO> listCabinetPending(Authentication authentication) {
    AgendaProPrincipal p = (AgendaProPrincipal) authentication.getPrincipal();
    return appointmentService.findPendingForCabinet(p);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public AppointmentDTO getById(Authentication authentication, @PathVariable Long id) {
    return appointmentService.findById(authentication, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public AppointmentDTO create(Authentication authentication, @RequestBody AppointmentDTO body) {
    return appointmentService.create(authentication, body);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public AppointmentDTO update(
      Authentication authentication, @PathVariable Long id, @RequestBody AppointmentDTO body) {
    return appointmentService.update(authentication, id, body);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public AppointmentDTO patchStatus(
      @PathVariable Long id,
      @RequestBody AppointmentStatusPatchDTO body,
      Authentication authentication) {
    AgendaProPrincipal p = (AgendaProPrincipal) authentication.getPrincipal();
    return appointmentService.patchStatus(id, body.getStatus(), p);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public void delete(Authentication authentication, @PathVariable Long id) {
    appointmentService.delete(authentication, id);
  }

  /**
   * Rendez-vous d'un patient.
   *
   * <p>Retourne la liste ordonnée par date de début décroissante.
   */
  @GetMapping("/patient/{patientId}")
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public List<AppointmentDTO> listByPatient(
      Authentication authentication, @PathVariable Long patientId) {
    return appointmentService.findByPatientId(authentication, patientId);
  }
}
