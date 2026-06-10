package com.medical.agenda.controller;

import com.medical.agenda.dto.MedicalRecordDTO;
import com.medical.agenda.service.MedicalRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/medical-record")
public class MedicalRecordController {

  private final MedicalRecordService medicalRecordService;

  public MedicalRecordController(MedicalRecordService medicalRecordService) {
    this.medicalRecordService = medicalRecordService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public MedicalRecordDTO getMedicalRecord(
      Authentication authentication,
      @PathVariable Long appointmentId) {
    return medicalRecordService.getMedicalRecord(authentication, appointmentId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public MedicalRecordDTO createMedicalRecord(
      Authentication authentication,
      @PathVariable Long appointmentId,
      @RequestBody MedicalRecordDTO dto) {
    return medicalRecordService.saveMedicalRecord(authentication, appointmentId, dto);
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('ASSISTANT','PRATICIEN','ADMIN')")
  public MedicalRecordDTO updateMedicalRecord(
      Authentication authentication,
      @PathVariable Long appointmentId,
      @RequestBody MedicalRecordDTO dto) {
    return medicalRecordService.saveMedicalRecord(authentication, appointmentId, dto);
  }
}
