package com.medical.agenda.service;

import com.medical.agenda.dto.AppointmentDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.entity.Doctor;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.AppointmentTypeRepository;
import com.medical.agenda.repository.DoctorRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final DoctorRepository doctorRepository;
  private final AppointmentTypeRepository appointmentTypeRepository;

  public AppointmentService(
      AppointmentRepository appointmentRepository,
      DoctorRepository doctorRepository,
      AppointmentTypeRepository appointmentTypeRepository) {
    this.appointmentRepository = appointmentRepository;
    this.doctorRepository = doctorRepository;
    this.appointmentTypeRepository = appointmentTypeRepository;
  }

  /** Fenêtre calendrier : chevauchements avec [rangeStart, rangeEnd). */
  @Transactional(readOnly = true)
  public List<AppointmentDTO> findInRange(Instant rangeStart, Instant rangeEnd) {
    if (!rangeStart.isBefore(rangeEnd)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start doit être avant end");
    }
    return appointmentRepository.findOverlappingRange(rangeStart, rangeEnd).stream()
        .map(AppointmentMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public AppointmentDTO findById(Long id) {
    Appointment a =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return AppointmentMapper.toDto(a);
  }

  /**
   * Création depuis l’agenda cabinet (assistant / praticien) : statut {@link AppointmentStatus#CONFIRMED}.
   */
  @Transactional
  public AppointmentDTO create(AppointmentDTO input) {
    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin inconnu"));

    AppointmentType type = resolveAppointmentType(input);

    Instant start = requireInstant(input.getStartTime(), "startTime");
    Integer duration = requirePositiveDuration(input.getDurationMinutes());
    Instant end = resolveEndTime(start, duration, input.getEndTime());

    Appointment entity = new Appointment();
    applyCommonFields(entity, input, doctor, type, start, end, duration);
    entity.setStatus(AppointmentStatus.CONFIRMED);

    return AppointmentMapper.toDto(appointmentRepository.save(entity));
  }

  @Transactional
  public AppointmentDTO update(Long id, AppointmentDTO input) {
    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin inconnu"));

    AppointmentType type = resolveAppointmentType(input);

    Instant start = requireInstant(input.getStartTime(), "startTime");
    Integer duration = requirePositiveDuration(input.getDurationMinutes());
    Instant end = resolveEndTime(start, duration, input.getEndTime());

    applyCommonFields(entity, input, doctor, type, start, end, duration);
    if (input.getStatus() != null) {
      entity.setStatus(input.getStatus());
    }

    return AppointmentMapper.toDto(appointmentRepository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    if (!appointmentRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    appointmentRepository.deleteById(id);
  }

  /**
   * Résout le type par {@code typeId} si présent et trouvé ; sinon par {@code typeCode}
   * (recommandé en complément côté client pour éviter les erreurs si l’id JSON perd en précision).
   */
  private AppointmentType resolveAppointmentType(AppointmentDTO input) {
    if (input.getTypeId() != null) {
      Optional<AppointmentType> byId = appointmentTypeRepository.findById(input.getTypeId());
      if (byId.isPresent()) {
        return byId.get();
      }
    }
    if (input.getTypeCode() != null && !input.getTypeCode().isBlank()) {
      String code = input.getTypeCode().trim().toUpperCase();
      return appointmentTypeRepository
          .findByCode(code)
          .orElseThrow(
              () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de visite inconnu (code)"));
    }
    if (input.getTypeId() != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Type de visite inconnu (typeId=" + input.getTypeId() + ")");
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeId ou typeCode requis");
  }

  private void applyCommonFields(
      Appointment entity,
      AppointmentDTO input,
      Doctor doctor,
      AppointmentType type,
      Instant start,
      Instant end,
      Integer duration) {
    entity.setTitle(requireText(input.getTitle(), "title"));
    entity.setAppointmentType(type);
    entity.setStartTime(start);
    entity.setEndTime(end);
    entity.setDurationMinutes(duration);
    entity.setDescription(input.getDescription() != null ? input.getDescription() : "");
    entity.setDoctor(doctor);
    entity.setColor(requireText(input.getColor(), "color"));
  }

  /** Si {@code endTime} est absent ou incohérent, recalcule à partir de {@code start} + durée. */
  private Instant resolveEndTime(Instant start, int durationMinutes, Instant requestedEnd) {
    Instant computed = start.plus(durationMinutes, ChronoUnit.MINUTES);
    if (requestedEnd == null) {
      return computed;
    }
    Instant tolerance = computed.plus(1, ChronoUnit.MINUTES);
    Instant toleranceNeg = computed.minus(1, ChronoUnit.MINUTES);
    if (requestedEnd.isBefore(toleranceNeg) || requestedEnd.isAfter(tolerance)) {
      return computed;
    }
    return requestedEnd;
  }

  private static Instant requireInstant(Instant value, String field) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " requis");
    }
    return value;
  }

  private static int requirePositiveDuration(Integer minutes) {
    if (minutes == null || minutes < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationMinutes doit être >= 1");
    }
    return minutes;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " requis");
    }
    return value.trim();
  }
}
