package com.medical.agenda.service;

import com.medical.agenda.dto.MedicalRecordDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.MedicalRecord;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.MedicalRecordRepository;
import com.medical.agenda.security.AgendaAccess;
import com.medical.agenda.security.AgendaProPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MedicalRecordService {

  private final MedicalRecordRepository medicalRecordRepository;
  private final AppointmentRepository appointmentRepository;

  public MedicalRecordService(
      MedicalRecordRepository medicalRecordRepository,
      AppointmentRepository appointmentRepository) {
    this.medicalRecordRepository = medicalRecordRepository;
    this.appointmentRepository = appointmentRepository;
  }

  @Transactional(readOnly = true)
  public MedicalRecordDTO getMedicalRecord(Authentication auth, Long appointmentId) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));

    assertAccess(auth, appointment);

    MedicalRecord medicalRecord = medicalRecordRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier médical introuvable"));

    return toDTO(medicalRecord);
  }

  @Transactional
  public MedicalRecordDTO saveMedicalRecord(Authentication auth, Long appointmentId, MedicalRecordDTO dto) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));

    assertAccess(auth, appointment);

    MedicalRecord medicalRecord = medicalRecordRepository.findById(appointmentId)
        .orElseGet(() -> {
          MedicalRecord newRecord = new MedicalRecord();
          newRecord.setAppointment(appointment);
          return newRecord;
        });

    medicalRecord.setAntecedents(dto.getAntecedents());
    medicalRecord.setSymptomes(dto.getSymptomes());
    medicalRecord.setConsultation(dto.getConsultation());
    medicalRecord.setDiagnostique(dto.getDiagnostique());
    medicalRecord.setConsommable(dto.getConsommable());
    medicalRecord.setActe(dto.getActe());
    medicalRecord.setRadiologie(dto.getRadiologie());

    MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
    return toDTO(saved);
  }

  private void assertAccess(Authentication auth, Appointment appointment) {
    if (auth == null || !auth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof AgendaProPrincipal pr) {
      if (AgendaAccess.isPlatformAdmin(pr)) {
        return;
      }
      Long orgId = appointment.getDoctor() != null ? appointment.getDoctor().getOrganizationId() : null;
      if (orgId == null || pr.organizationId() == null || !orgId.equals(pr.organizationId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé - Hors cabinet");
      }
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux praticiens et assistants du cabinet");
    }
  }

  private MedicalRecordDTO toDTO(MedicalRecord medicalRecord) {
    MedicalRecordDTO dto = new MedicalRecordDTO();
    dto.setAppointmentId(medicalRecord.getId());
    dto.setAntecedents(medicalRecord.getAntecedents());
    dto.setSymptomes(medicalRecord.getSymptomes());
    dto.setConsultation(medicalRecord.getConsultation());
    dto.setDiagnostique(medicalRecord.getDiagnostique());
    dto.setConsommable(medicalRecord.getConsommable());
    dto.setActe(medicalRecord.getActe());
    dto.setRadiologie(medicalRecord.getRadiologie());
    return dto;
  }
}
