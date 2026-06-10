package com.medical.agenda.service;

import com.medical.agenda.dto.AppointmentDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import com.medical.agenda.entity.AppointmentType;

public final class AppointmentMapper {

  private AppointmentMapper() {}

  public static AppointmentDTO toDto(Appointment entity) {
    AppointmentDTO d = new AppointmentDTO();
    d.setId(entity.getId());
    d.setTitle(entity.getTitle());
    d.setPatientId(entity.getPatientId());
    d.setPatientPrenom(entity.getPatientPrenom());
    d.setPatientNom(entity.getPatientNom());
    d.setVisitReasonCode(entity.getVisitReasonCode());

    AppointmentType type = entity.getAppointmentType();
    if (type != null) {
      d.setTypeId(type.getId());
      d.setTypeCode(type.getCode());
      d.setTypeLabel(type.getLabel());
      d.setTypeColor(type.getColorCode());
    } else {
      d.setTypeCode("UNKNOWN");
      d.setTypeLabel("Inconnu");
      d.setTypeColor("#64748b");
    }

    d.setStartTime(entity.getStartTime());
    d.setEndTime(entity.getEndTime());
    d.setDurationMinutes(entity.getDurationMinutes());
    d.setDescription(entity.getDescription());
    if (entity.getDoctor() != null) {
      d.setDoctorId(entity.getDoctor().getId());
      d.setDoctorName(entity.getDoctor().getName());
      d.setDoctorSpecialty(entity.getDoctor().getSpecialty());
      d.setDoctorExternalPractitionerId(entity.getDoctor().getExternalPractitionerId());
    } else {
      d.setDoctorId(null);
      d.setDoctorName("Médecin inconnu");
      d.setDoctorSpecialty(null);
      d.setDoctorExternalPractitionerId(null);
    }
    d.setColor(entity.getColor());
    d.setStatus(
        entity.getStatus() != null ? entity.getStatus() : AppointmentStatus.CONFIRMED);
    d.setLocationMode(entity.getLocationMode());
    return d;
  }
}
