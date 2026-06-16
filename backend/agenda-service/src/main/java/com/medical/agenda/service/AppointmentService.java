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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.medical.agenda.dto.AppointmentEvent;

@Service
public class AppointmentService {

  private static final String ERROR_DOCTOR_UNKNOWN = "Médecin inconnu";
  private static final String ERROR_ACCESS_DENIED = "Accès refusé";
  private static final String START_TIME_FIELD = "startTime";
  private static final String ERROR_OUT_OF_MEDICAL_ORGANIZATION = "Rendez-vous hors cabinet";

  private final AppointmentRepository appointmentRepository;
  private final DoctorRepository doctorRepository;
  private final AppointmentTypeRepository appointmentTypeRepository;
  private final CabinetHoursClient medicalOrganizationHoursClient;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public AppointmentService(
      AppointmentRepository appointmentRepository,
      DoctorRepository doctorRepository,
      AppointmentTypeRepository appointmentTypeRepository,
      CabinetHoursClient medicalOrganizationHoursClient,
      KafkaTemplate<String, Object> kafkaTemplate) {
    this.appointmentRepository = appointmentRepository;
    this.doctorRepository = doctorRepository;
    this.appointmentTypeRepository = appointmentTypeRepository;
    this.medicalOrganizationHoursClient = medicalOrganizationHoursClient;
    this.kafkaTemplate = kafkaTemplate;
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_ACCESS_DENIED);
      }
      return appointmentRepository.findPatientAppointmentRows(patientId).stream()
          .map(AppointmentService::toPatientAppointmentDto)
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_ACCESS_DENIED);
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
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_OUT_OF_MEDICAL_ORGANIZATION);
      }
      return AppointmentMapper.toDto(a);
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  /**
   * Création depuis l’agenda medicalOrganization (assistant / praticien) : statut {@link AppointmentStatus#CONFIRMED}.
   */
  @Transactional
  public AppointmentDTO create(Authentication auth, AppointmentDTO input) {
    AgendaProPrincipal principal = requireProPrincipal(auth);
    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ERROR_DOCTOR_UNKNOWN));
    assertMedicalOrganizationMayManageDoctor(principal, doctor);

    AppointmentType type = resolveAppointmentType(input);

    Instant start = requireInstant(input.getStartTime(), START_TIME_FIELD);
    Integer duration = requirePositiveDuration(input.getDurationMinutes());
    Instant end = resolveEndTime(start, duration, input.getEndTime());

    Appointment entity = new Appointment();
    applyCommonFields(entity, input, doctor, type, start, end, duration);
    entity.setStatus(AppointmentStatus.CONFIRMED);

    Appointment saved = appointmentRepository.save(entity);
    publishAppointmentEvent(saved);
    return AppointmentMapper.toDto(saved);
  }

  /**
   * Demande de rendez-vous initiée par le patient (questionnaire + créneau) — statut
   * {@link AppointmentStatus#PENDING} jusqu'à validation medicalOrganization.
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
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ERROR_DOCTOR_UNKNOWN));

    AppointmentDTO typeBridge = new AppointmentDTO();
    typeBridge.setTypeCode(in.getTypeCode());
    AppointmentType type = resolveAppointmentType(typeBridge);

    Instant start = requireInstant(in.getStartTime(), START_TIME_FIELD);
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

    String prenom = in.getPatientPrenom() != null ? in.getPatientPrenom().trim() : "";
    String nom = in.getPatientNom() != null ? in.getPatientNom().trim() : "";
    String patientName = (prenom + " " + nom).trim();
    String typeLabel = type.getLabel() != null ? type.getLabel().trim() : "Consultation";
    String generatedTitle = patientName.isEmpty() ? typeLabel : typeLabel + " — " + patientName;

    bridge.setTitle(generatedTitle);
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
    entity.setLocationMode(trimToNull(in.getLocationMode()));
    Appointment saved = appointmentRepository.save(entity);
    publishAppointmentEvent(saved);
    return AppointmentMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public com.medical.agenda.dto.AgendaSlotsDTO getAvailableSlots(Long doctorId, String dateString, int duration) {
    return getAvailableSlots(doctorId, dateString, duration, null);
  }

  @Transactional(readOnly = true)
  public com.medical.agenda.dto.AgendaSlotsDTO getAvailableSlots(Long doctorId, String dateString, int duration, String timezoneStr) {
    List<String> available = new ArrayList<>();
    LocalDate date = LocalDate.parse(dateString);
    
    Doctor doctor = doctorRepository.findById(doctorId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_DOCTOR_UNKNOWN));

    List<CabinetHoursClient.CabinetHoraireDTO> horaires = 
        medicalOrganizationHoursClient.getCabinetHoraires(doctor.getOrganizationId());

    // 1 (Lundi) -> 7 (Dimanche)
    int dayOfWeek = date.getDayOfWeek().getValue();
    String dayOfWeekStr = switch (dayOfWeek) {
      case 1 -> "LUNDI";
      case 2 -> "MARDI";
      case 3 -> "MERCREDI";
      case 4 -> "JEUDI";
      case 5 -> "VENDREDI";
      case 6 -> "SAMEDI";
      case 7 -> "DIMANCHE";
      default -> "";
    };
    
    List<CabinetHoursClient.CabinetHoraireDTO> dayHoraires = horaires.stream()
        .filter(h -> dayOfWeekStr.equals(h.getJour()))
        .toList();

    List<String> standardSlots = calculateStandardSlots(dayOfWeek, horaires, dayHoraires, duration);

    ZoneId zoneId = ZoneId.systemDefault();
    if (timezoneStr != null && !timezoneStr.isBlank()) {
      try {
        zoneId = ZoneId.of(timezoneStr);
      } catch (Exception e) {
        // fallback to systemDefault if invalid zone id passed
      }
    }

    Instant now = Instant.now();
    for (String time : standardSlots) {
      java.time.LocalTime t = java.time.LocalTime.parse(time);
      java.time.LocalDateTime ldt = date.atTime(t);
      Instant start = ldt.atZone(zoneId).toInstant();
      Instant end = start.plus(duration, ChronoUnit.MINUTES);

      // A slot is available only if it starts in the future AND there is no overlapping confirmed booking
      if (start.isAfter(now) && !appointmentRepository.existsBlockingOverlap(doctorId, start, end)) {
        available.add(time);
      }
    }
    return new com.medical.agenda.dto.AgendaSlotsDTO(standardSlots, available);
  }

  private List<String> calculateStandardSlots(int dayOfWeek, List<CabinetHoursClient.CabinetHoraireDTO> horaires, List<CabinetHoursClient.CabinetHoraireDTO> dayHoraires, int duration) {
    List<String> standardSlots = new ArrayList<>();
    if (horaires.isEmpty()) {
      // Fallback if no horaires configured AT ALL for this medicalOrganization
      // Only apply fallback on Monday to Friday
      if (dayOfWeek >= 1 && dayOfWeek <= 5) {
        String[] defaults = {
          "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
          "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
        };
        standardSlots.addAll(List.of(defaults));
      }
    } else if (dayHoraires.isEmpty()) {
      // MedicalOrganization has hours configured, but NONE for this specific day -> MedicalOrganization is closed
      // standardSlots remains empty
    } else {
      for (CabinetHoursClient.CabinetHoraireDTO h : dayHoraires) {
        java.time.LocalTime startPeriod = java.time.LocalTime.parse(h.getHeureDebut());
        java.time.LocalTime endPeriod = java.time.LocalTime.parse(h.getHeureFin());
        
        java.time.LocalTime current = startPeriod;
        while (current.plusMinutes(duration).isBefore(endPeriod) || current.plusMinutes(duration).equals(endPeriod)) {
          standardSlots.add(current.toString());
          current = current.plusMinutes(duration);
        }
      }
    }
    return standardSlots;
  }

  private static String buildPatientBookingDescription(PatientBookingRequestDTO in) {
    StringBuilder sb = new StringBuilder();
    sb.append("Rendez-vous réservé en ligne par le patient");

    appendBeneficiaryInfo(sb, in);
    appendLocationInfo(sb, in);

    sb.append(". ");

    String prior = humanizeText(in.getPriorCareCode());
    if (!prior.isEmpty()) {
      if (prior.toLowerCase().contains("nouveau")) {
        sb.append("Il s'agit d'une première consultation (nouveau patient). ");
      } else {
        sb.append("Patient déjà suivi (").append(prior.toLowerCase()).append("). ");
      }
    }

    String visit = humanizeText(in.getVisitReasonCode());
    if (!visit.isEmpty()) {
      sb.append("Motif de la visite : ").append(visit.toLowerCase()).append(". ");
    }

    if (in.getReferredBy() != null && !in.getReferredBy().isBlank()) {
      sb.append("Adressé par le Dr ").append(in.getReferredBy().trim()).append(". ");
    }

    if (in.getMapQuery() != null && !in.getMapQuery().isBlank()) {
      sb.append("\nAdresse renseignée : ").append(in.getMapQuery().trim());
    }

    return sb.toString().trim();
  }

  private static void appendBeneficiaryInfo(StringBuilder sb, PatientBookingRequestDTO in) {
    String benef = humanizeBeneficiary(in.getBeneficiarySummary());
    if (!benef.isEmpty()) {
      sb.append(" pour ").append(benef.toLowerCase());
    } else {
      sb.append(" pour lui-même");
    }
  }

  private static void appendLocationInfo(StringBuilder sb, PatientBookingRequestDTO in) {
    String location = humanizeLocation(in.getLocationMode());
    if (!location.isEmpty()) {
      if (location.startsWith("Au") || location.startsWith("En")) {
        sb.append(", prévu ").append(location.toLowerCase());
      } else {
        sb.append(", prévu en ").append(location.toLowerCase());
      }
    }
  }

  private static String humanizeLocation(String code) {
    if (code == null || code.isBlank()) return "";
    return switch (code.trim().toUpperCase()) {
      case "CABINET" -> "Au cabinet";
      case "CLINIC"  -> "En clinique";
      case "REMOTE"  -> "Téléconsultation";
      default        -> code.trim();
    };
  }

  private static String humanizeBeneficiary(String raw) {
    if (raw == null || raw.isBlank()) return "";
    String trimmed = raw.trim();
    if ("self".equalsIgnoreCase(trimmed)) return "Lui-même";
    if (trimmed.toLowerCase().startsWith("relative")) return "Un proche";
    return trimmed;
  }

  private static String humanizeText(String raw) {
    if (raw == null || raw.isBlank()) return "";
    return raw.trim().replace('_', ' ');
  }

  @Transactional
  public AppointmentDTO update(Authentication auth, Long id, AppointmentDTO input) {
    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    AgendaProPrincipal principal = requireProPrincipal(auth);
    assertMedicalOrganizationMayManageAppointment(principal, entity);

    Doctor doctor =
        doctorRepository
            .findById(input.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ERROR_DOCTOR_UNKNOWN));

    assertMedicalOrganizationMayManageDoctor(principal, doctor);

    AppointmentType type = resolveAppointmentType(input);

    Instant start = requireInstant(input.getStartTime(), START_TIME_FIELD);
    Integer duration = requirePositiveDuration(input.getDurationMinutes());
    Instant end = resolveEndTime(start, duration, input.getEndTime());

    applyCommonFields(entity, input, doctor, type, start, end, duration);
    if (input.getStatus() != null) {
      entity.setStatus(input.getStatus());
    }

    Appointment saved = appointmentRepository.save(entity);
    publishAppointmentEvent(saved);
    return AppointmentMapper.toDto(saved);
  }

  @Transactional
  public void delete(Authentication auth, Long id) {
    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    AgendaProPrincipal principal = requireProPrincipal(auth);
    assertMedicalOrganizationMayManageAppointment(principal, entity);
    appointmentRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<AppointmentDTO> findPendingForCabinet(AgendaProPrincipal principal) {
    Long organizationId = principal != null ? principal.organizationId() : null;
    if (organizationId == null) {
      return List.of();
    }
    return appointmentRepository
        .findByDoctorOrganizationIdAndStatusOrderByStartTimeAsc(
            organizationId, AppointmentStatus.PENDING)
        .stream()
        .map(AppointmentMapper::toDto)
        .toList();
  }

  @Transactional
  public AppointmentDTO patchStatus(Long id, AppointmentStatus newStatus, AgendaProPrincipal principal) {
    validatePatchAuthorization(principal);
    if (newStatus == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status requis");
    }

    Appointment entity =
        appointmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    validatePatchOrganizationAccess(principal, entity);
    validateStatusTransition(entity.getStatus(), newStatus);

    entity.setStatus(newStatus);
    Appointment saved = appointmentRepository.save(entity);
    publishAppointmentEvent(saved);
    return AppointmentMapper.toDto(saved);
  }

  private void validatePatchAuthorization(AgendaProPrincipal principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_ACCESS_DENIED);
    }
    String role = principal.role();
    if (role == null
        || (!"ASSISTANT".equals(role)
            && !"PRATICIEN".equals(role)
            && !"ADMIN".equals(role))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle non autorisé");
    }
  }

  private void validatePatchOrganizationAccess(AgendaProPrincipal principal, Appointment entity) {
    if (entity.getDoctor() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_OUT_OF_MEDICAL_ORGANIZATION);
    }
    assertOrganizationAccess(
        principal, entity.getDoctor().getOrganizationId(), ERROR_OUT_OF_MEDICAL_ORGANIZATION);
  }

  private static void validateStatusTransition(AppointmentStatus current, AppointmentStatus newStatus) {
    if (current == AppointmentStatus.PENDING) {
      if (newStatus != AppointmentStatus.CONFIRMED && newStatus != AppointmentStatus.CANCELLED) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Un RDV en attente ne peut être que CONFIRMED ou CANCELLED.");
      }
    } else if (current == AppointmentStatus.CONFIRMED) {
      if (newStatus != AppointmentStatus.COMPLETED
          && newStatus != AppointmentStatus.NO_SHOW
          && newStatus != AppointmentStatus.CANCELLED) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Un RDV confirmé ne peut être que COMPLETED, NO_SHOW ou CANCELLED.");
      }
    } else {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ce rendez-vous ne peut plus être modifié.");
    }
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

  private static AppointmentDTO toPatientAppointmentDto(Object[] row) {
    AppointmentDTO d = new AppointmentDTO();
    d.setId(toLong(row, 0));
    d.setTitle(toString(row, 1));
    d.setPatientId(toLong(row, 2));
    d.setPatientPrenom(toString(row, 3));
    d.setPatientNom(toString(row, 4));
    d.setVisitReasonCode(toString(row, 5));
    d.setTypeId(toLong(row, 6));
    d.setTypeCode(defaultString(toString(row, 7), "UNKNOWN"));
    d.setTypeLabel(defaultString(toString(row, 8), "Inconnu"));
    d.setTypeColor(defaultString(toString(row, 9), "#64748b"));
    d.setStartTime(toInstant(row, 10));
    d.setEndTime(toInstant(row, 11));
    d.setDurationMinutes(toInteger(row, 12));
    d.setDescription(defaultString(toString(row, 13), ""));
    d.setDoctorId(toLong(row, 14));
    d.setColor(defaultString(toString(row, 15), "#64748b"));
    d.setDoctorName(defaultString(toString(row, 16), ERROR_DOCTOR_UNKNOWN));
    d.setDoctorSpecialty(toString(row, 17));
    d.setDoctorExternalPractitionerId(toLong(row, 18));
    d.setStatus(parseStatus(row, 19));
    d.setLocationMode(toString(row, 20));
    return d;
  }

  private static Long toLong(Object[] row, int index) {
    Object value = row[index];
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.valueOf(value.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private static Integer toInteger(Object[] row, int index) {
    Object value = row[index];
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.valueOf(value.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private static String toString(Object[] row, int index) {
    Object value = row[index];
    return value != null ? value.toString() : null;
  }

  private static String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static Instant toInstant(Object[] row, int index) {
    Object value = row[index];
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    if (value instanceof java.time.OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof java.time.LocalDateTime ldt) {
      return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }
    try {
      return Instant.parse(value.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private static AppointmentStatus parseStatus(Object[] row, int index) {
    String raw = toString(row, index);
    if (raw == null || raw.isBlank()) {
      return AppointmentStatus.CONFIRMED;
    }
    try {
      return AppointmentStatus.valueOf(raw.trim().toUpperCase());
    } catch (Exception e) {
      return AppointmentStatus.CONFIRMED;
    }
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

  private void assertMedicalOrganizationMayManageDoctor(AgendaProPrincipal principal, Doctor doctor) {
    assertOrganizationAccess(principal, doctor.getOrganizationId(), "MÃ©decin hors medicalOrganization");
  }

  private void assertMedicalOrganizationMayManageAppointment(
      AgendaProPrincipal principal, Appointment entity) {
    Long orgId = entity.getDoctor() != null ? entity.getDoctor().getOrganizationId() : null;
    if (orgId == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_OUT_OF_MEDICAL_ORGANIZATION);
    }
    assertOrganizationAccess(principal, orgId, ERROR_OUT_OF_MEDICAL_ORGANIZATION);
  }

  private void assertOrganizationAccess(
      AgendaProPrincipal principal, Long organizationId, String forbiddenMessage) {
    if (AgendaAccess.isPlatformAdmin(principal)) {
      return;
    }
    if (principal.organizationId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_ACCESS_DENIED);
    }
    if (organizationId == null || !organizationId.equals(principal.organizationId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }
  }

  private void publishAppointmentEvent(Appointment appointment) {
    try {
      Long practitionerId = appointment.getDoctor() != null ? appointment.getDoctor().getExternalPractitionerId() : null;
      Long organizationId = appointment.getDoctor() != null ? appointment.getDoctor().getOrganizationId() : null;
      Double fee = (appointment.getAppointmentType() != null && appointment.getAppointmentType().getPrice() != null)
          ? appointment.getAppointmentType().getPrice().doubleValue()
          : null;
      String dateTimeStr = appointment.getStartTime() != null ? appointment.getStartTime().toString() : null;
      String statusStr = appointment.getStatus() != null ? appointment.getStatus().name() : null;

      AppointmentEvent event = new AppointmentEvent(
          appointment.getId(),
          appointment.getPatientId(),
          practitionerId,
          organizationId,
          dateTimeStr,
          statusStr,
          fee
      );
      kafkaTemplate.send("appointment-events", event.appointmentId().toString(), event);
    } catch (Exception e) {
      System.err.println("Failed to send Kafka appointment event: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
