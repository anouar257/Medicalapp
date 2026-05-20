package com.medical.agenda.service;

import com.medical.agenda.dto.AppointmentDTO;
import com.medical.agenda.dto.PatientBookingRequestDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.entity.Doctor;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.AppointmentTypeRepository;
import com.medical.agenda.repository.DoctorRepository;
import com.medical.agenda.security.AgendaAccess;
import com.medical.agenda.security.AgendaPatientPrincipal;
import com.medical.agenda.security.AgendaProPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
  public List<AppointmentDTO> findInRange(Authentication auth, Instant rangeStart, Instant rangeEnd) {
    if (!rangeStart.isBefore(rangeEnd)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start doit être avant end");
    }
    AgendaProPrincipal principal = requireProPrincipal(auth);
    List<Appointment> rows;
    if (AgendaAccess.isPlatformAdmin(principal)) {
      rows = appointmentRepository.findOverlappingRange(rangeStart, rangeEnd);
    } else if (principal.organizationId() == null) {
      return List.of();
    } else {
      rows =
          appointmentRepository.findOverlappingRangeForCabinet(
              rangeStart, rangeEnd, principal.organizationId());
    }
    return rows.stream().map(AppointmentMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<AppointmentDTO> findByPatientId(Authentication auth, Long patientId) {
    if (patientId == null) {
      return List.of();
    }
    Object p = requireAnyPrincipal(auth);
    if (p instanceof AgendaPatientPrincipal pp) {
      if (!pp.patientId().equals(patientId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
      }
      return appointmentRepository.findByPatientIdOrderByStartTimeDesc(patientId).stream()
          .map(AppointmentMapper::toDto)
          .toList();
    }
    if (p instanceof AgendaProPrincipal pr) {
      if (AgendaAccess.isPlatformAdmin(pr)) {
        return appointmentRepository.findByPatientIdOrderByStartTimeDesc(patientId).stream()
            .map(AppointmentMapper::toDto)
            .toList();
      }
      if (pr.organizationId() == null) {
        return List.of();
      }
      return appointmentRepository
          .findByPatientIdAndDoctorOrganizationId(patientId, pr.organizationId())
          .stream()
          .map(AppointmentMapper::toDto)
          .toList();
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  @Transactional(readOnly = true)
  public AppointmentDTO findById(Authentication auth, Long id) {
    Appointment a =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Object p = requireAnyPrincipal(auth);
    if (p instanceof AgendaPatientPrincipal pp) {
      if (a.getPatientId() == null || !a.getPatientId().equals(pp.patientId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
      }
      return AppointmentMapper.toDto(a);
    }
    if (p instanceof AgendaProPrincipal pr) {
      if (AgendaAccess.isPlatformAdmin(pr)) {
        return AppointmentMapper.toDto(a);
      }
      Long orgId = a.getDoctor() != null ? a.getDoctor().getOrganizationId() : null;
      if (orgId == null
          || pr.organizationId() == null
          || !orgId.equals(pr.organizationId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rendez-vous hors cabinet");
      }
      return AppointmentMapper.toDto(a);
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  /**
   * Création depuis l’agenda cabinet (assistant / praticien) : statut {@link AppointmentStatus#CONFIRMED}.
   */
  @Transactional
  public AppointmentDTO create(Authentication auth, AppointmentDTO input) {
    AgendaProPrincipal principal = requireProPrincipal(auth);
    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin inconnu"));
    assertCabinetMayManageDoctor(principal, doctor);

    AppointmentType type = resolveAppointmentType(input);

    Instant start = requireInstant(input.getStartTime(), "startTime");
    Integer duration = requirePositiveDuration(input.getDurationMinutes());
    Instant end = resolveEndTime(start, duration, input.getEndTime());

    Appointment entity = new Appointment();
    applyCommonFields(entity, input, doctor, type, start, end, duration);
    entity.setStatus(AppointmentStatus.CONFIRMED);

    return AppointmentMapper.toDto(appointmentRepository.save(entity));
  }

  /**
   * Demande de rendez-vous initiée par le patient (questionnaire + créneau) — statut
   * {@link AppointmentStatus#PENDING} jusqu'à validation cabinet.
   */
  @Transactional
  public AppointmentDTO createPatientBooking(Authentication auth, PatientBookingRequestDTO in) {
    AgendaPatientPrincipal pp = requirePatientPrincipal(auth);
    if (!pp.patientId().equals(in.getPatientId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "patientId incohérent avec le jeton");
    }
    Doctor doctor =
        doctorRepository
            .findById(in.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin inconnu"));

    AppointmentDTO typeBridge = new AppointmentDTO();
    typeBridge.setTypeCode(in.getTypeCode());
    AppointmentType type = resolveAppointmentType(typeBridge);

    Instant start = requireInstant(in.getStartTime(), "startTime");
    int duration =
        in.getDurationMinutes() != null && in.getDurationMinutes() > 0
            ? requirePositiveDuration(in.getDurationMinutes())
            : requirePositiveDuration(type.getDefaultDurationMinutes());
    Instant end = start.plus(duration, ChronoUnit.MINUTES);

    if (appointmentRepository.existsBlockingOverlap(in.getDoctorId(), start, end)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ce créneau chevauche un rendez-vous existant du praticien.");
    }

    AppointmentDTO bridge = new AppointmentDTO();
    bridge.setTitle(requireText(in.getTitle(), "title"));
    bridge.setPatientId(in.getPatientId());
    bridge.setTypeId(type.getId());
    bridge.setStartTime(start);
    bridge.setDurationMinutes(duration);
    bridge.setDescription(buildPatientBookingDescription(in));
    bridge.setDoctorId(doctor.getId());
    bridge.setColor(requireText(in.getColor(), "color"));

    Appointment entity = new Appointment();
    applyCommonFields(entity, bridge, doctor, type, start, end, duration);
    entity.setStatus(AppointmentStatus.PENDING);
    entity.setPatientPrenom(trimToNull(in.getPatientPrenom()));
    entity.setPatientNom(trimToNull(in.getPatientNom()));
    entity.setVisitReasonCode(trimToNull(in.getVisitReasonCode()));
    return AppointmentMapper.toDto(appointmentRepository.save(entity));
  }

  private static String buildPatientBookingDescription(PatientBookingRequestDTO in) {
    StringBuilder sb = new StringBuilder();
    sb.append("PATIENT_WEB;");
    sb.append("location=").append(safeToken(in.getLocationMode())).append(';');
    sb.append("prior=").append(safeToken(in.getPriorCareCode())).append(';');
    sb.append("visit=").append(safeToken(in.getVisitReasonCode())).append(';');
    sb.append("benef=").append(safeToken(in.getBeneficiarySummary())).append(';');
    if (in.getReferredBy() != null && !in.getReferredBy().isBlank()) {
      sb.append("referredBy=").append(safeToken(in.getReferredBy())).append(';');
    }
    if (in.getMapQuery() != null && !in.getMapQuery().isBlank()) {
      sb.append("map=").append(safeToken(in.getMapQuery()));
    }
    return sb.toString();
  }

  private static String safeToken(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.trim().replace(';', ',').replace('\n', ' ');
  }

  @Transactional
  public AppointmentDTO update(Authentication auth, Long id, AppointmentDTO input) {
    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    AgendaProPrincipal principal = requireProPrincipal(auth);
    assertCabinetMayManageAppointment(principal, entity);

    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médecin inconnu"));

    assertCabinetMayManageDoctor(principal, doctor);

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
  public void delete(Authentication auth, Long id) {
    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    AgendaProPrincipal principal = requireProPrincipal(auth);
    assertCabinetMayManageAppointment(principal, entity);
    appointmentRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<AppointmentDTO> findPendingForCabinet(AgendaProPrincipal principal) {
    Long organizationId = principal != null ? principal.organizationId() : null;
    if (organizationId == null) {
      return List.of();
    }
    return appointmentRepository
        .findByDoctor_OrganizationIdAndStatusOrderByStartTimeAsc(
            organizationId, AppointmentStatus.PENDING)
        .stream()
        .map(AppointmentMapper::toDto)
        .toList();
  }

  @Transactional
  public AppointmentDTO patchStatus(Long id, AppointmentStatus newStatus, AgendaProPrincipal principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
    }
    String role = principal.role();
    if (role == null
        || (!"ASSISTANT".equals(role)
            && !"PRATICIEN".equals(role)
            && !"ADMIN".equals(role))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé");
    }
    if (newStatus == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status requis");
    }
    if (newStatus != AppointmentStatus.CONFIRMED && newStatus != AppointmentStatus.CANCELLED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Statut invalide (CONFIRMED ou CANCELLED attendu)");
    }

    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!AgendaAccess.isPlatformAdmin(principal)) {
      if (principal.organizationId() == null) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
      }
      Long orgId = entity.getDoctor().getOrganizationId();
      if (orgId == null || !orgId.equals(principal.organizationId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rendez-vous hors cabinet");
      }
    }
    if (entity.getStatus() != AppointmentStatus.PENDING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Seul un rendez-vous en attente peut être modifié.");
    }

    entity.setStatus(newStatus);
    return AppointmentMapper.toDto(appointmentRepository.save(entity));
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
    entity.setPatientId(input.getPatientId());
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

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String t = value.trim();
    return t.isEmpty() ? null : t;
  }

  private static AgendaProPrincipal requireProPrincipal(Authentication auth) {
    Object p = requireAnyPrincipal(auth);
    if (p instanceof AgendaProPrincipal pr) {
      return pr;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux comptes professionnels");
  }

  private static AgendaPatientPrincipal requirePatientPrincipal(Authentication auth) {
    Object p = requireAnyPrincipal(auth);
    if (p instanceof AgendaPatientPrincipal pp) {
      return pp;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux patients");
  }

  private static Object requireAnyPrincipal(Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    Object p = auth.getPrincipal();
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return p;
  }

  private void assertCabinetMayManageDoctor(AgendaProPrincipal principal, Doctor doctor) {
    if (AgendaAccess.isPlatformAdmin(principal)) {
      return;
    }
    if (principal.organizationId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
    }
    Long orgId = doctor.getOrganizationId();
    if (orgId == null || !orgId.equals(principal.organizationId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Médecin hors cabinet");
    }
  }

  private void assertCabinetMayManageAppointment(AgendaProPrincipal principal, Appointment entity) {
    if (AgendaAccess.isPlatformAdmin(principal)) {
      return;
    }
    if (principal.organizationId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
    }
    Long orgId = entity.getDoctor() != null ? entity.getDoctor().getOrganizationId() : null;
    if (orgId == null || !orgId.equals(principal.organizationId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rendez-vous hors cabinet");
    }
  }
}
