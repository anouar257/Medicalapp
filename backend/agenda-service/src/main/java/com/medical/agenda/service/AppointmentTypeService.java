package com.medical.agenda.service;

import com.medical.agenda.dto.AppointmentTypeDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.AppointmentTypeRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentTypeService {

  private final AppointmentRepository appointmentRepository;
  private final AppointmentTypeRepository repository;

  public AppointmentTypeService(
      AppointmentRepository appointmentRepository, AppointmentTypeRepository repository) {
    this.appointmentRepository = appointmentRepository;
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<AppointmentTypeDTO> findAll() {
    return repository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AppointmentTypeService::toDto)
        .toList();
  }

  @Transactional
  public AppointmentTypeDTO create(AppointmentTypeDTO input) {
    String code = requireText(input.getCode(), "code").toUpperCase();
    if (repository.existsByCode(code)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Code déjà utilisé : " + code);
    }
    AppointmentType e = new AppointmentType();
    e.setCode(code);
    e.setLabel(requireText(input.getLabel(), "label"));
    e.setColorCode(requireText(input.getColorCode(), "colorCode"));
    e.setDefaultDurationMinutes(requirePositiveDuration(input.getDefaultDurationMinutes()));
    e.setDisplayOrder(input.getDisplayOrder() != null ? input.getDisplayOrder() : 100);
    e.setActive(input.isActive());
    e.setPrice(input.getPrice());
    e.setPriceVariable(input.isPriceVariable());
    e.setSourcePractitionerId(input.getSourcePractitionerId());
    e.setSourceActId(input.getSourceActId());
    return toDto(repository.save(e));
  }

  @Transactional
  public AppointmentTypeDTO update(Long id, AppointmentTypeDTO input) {
    AppointmentType e =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type inconnu."));
    if (input.getLabel() != null && !input.getLabel().isBlank()) {
      e.setLabel(input.getLabel().trim());
    }
    if (input.getColorCode() != null && !input.getColorCode().isBlank()) {
      e.setColorCode(input.getColorCode().trim());
    }
    if (input.getDefaultDurationMinutes() != null) {
      e.setDefaultDurationMinutes(requirePositiveDuration(input.getDefaultDurationMinutes()));
    }
    if (input.getDisplayOrder() != null) {
      e.setDisplayOrder(input.getDisplayOrder());
    }
    e.setActive(input.isActive());
    e.setPrice(input.getPrice());
    e.setPriceVariable(input.isPriceVariable());
    e.setSourcePractitionerId(input.getSourcePractitionerId());
    e.setSourceActId(input.getSourceActId());
    return toDto(repository.save(e));
  }

  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Type inconnu.");
    }
    repository.deleteById(id);
  }

  @Transactional
  public AppointmentTypeDTO syncFromAct(AppointmentTypeDTO input) {
    String code = requireText(input.getCode(), "code").toUpperCase();
    AppointmentType e = repository.findByCode(code).orElseGet(AppointmentType::new);
    e.setCode(code);
    e.setLabel(requireText(input.getLabel(), "label"));
    e.setColorCode(requireText(input.getColorCode(), "colorCode"));
    e.setDefaultDurationMinutes(requirePositiveDuration(input.getDefaultDurationMinutes()));
    e.setDisplayOrder(input.getDisplayOrder() != null ? input.getDisplayOrder() : 100);
    e.setActive(input.isActive());
    e.setPrice(input.getPrice());
    e.setPriceVariable(input.isPriceVariable());
    e.setSourcePractitionerId(input.getSourcePractitionerId());
    e.setSourceActId(input.getSourceActId());
    AppointmentType saved = repository.save(e);
    rebindHistoricalAppointments(saved);
    cleanupLegacyTypes();
    return toDto(saved);
  }

  @Transactional
  public void deactivateByCode(String code) {
    AppointmentType e = repository.findByCode(requireText(code, "code").toUpperCase()).orElse(null);
    if (e == null) {
      return;
    }
    e.setActive(false);
    repository.save(e);
    cleanupLegacyTypes();
  }

  public static AppointmentTypeDTO toDto(AppointmentType e) {
    AppointmentTypeDTO dto = new AppointmentTypeDTO();
    dto.setId(e.getId());
    dto.setCode(e.getCode());
    dto.setLabel(e.getLabel());
    dto.setColorCode(e.getColorCode());
    dto.setDefaultDurationMinutes(e.getDefaultDurationMinutes() != null ? e.getDefaultDurationMinutes() : 15);
    dto.setDisplayOrder(e.getDisplayOrder() != null ? e.getDisplayOrder() : 100);
    dto.setActive(e.isActive());
    dto.setPrice(e.getPrice());
    dto.setPriceVariable(e.isPriceVariable());
    dto.setSourcePractitionerId(e.getSourcePractitionerId());
    dto.setSourceActId(e.getSourceActId());
    return dto;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " requis");
    }
    return value.trim();
  }

  private static int requirePositiveDuration(Integer minutes) {
    if (minutes == null || minutes < 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "defaultDurationMinutes doit être >= 1");
    }
    return minutes;
  }

  private void rebindHistoricalAppointments(AppointmentType type) {
    if (type.getId() == null
        || type.getSourcePractitionerId() == null
        || type.getLabel() == null
        || type.getLabel().isBlank()) {
      return;
    }
    appointmentRepository.rebindLegacyAppointmentsToDynamicType(
        type.getId(), type.getSourcePractitionerId(), type.getLabel());
    rebindLegacyAppointmentsByDuration(type.getSourcePractitionerId());
    refreshDynamicAppointments(type.getSourcePractitionerId());
  }

  private void cleanupLegacyTypes() {
    repository.deleteUnreferencedLegacyTypes();
  }

  private void rebindLegacyAppointmentsByDuration(Long sourcePractitionerId) {
    if (sourcePractitionerId == null) {
      return;
    }

    List<AppointmentType> dynamicTypes =
        repository.findBySourcePractitionerIdOrderByDisplayOrderAscIdAsc(sourcePractitionerId).stream()
            .filter(type -> type.getSourceActId() != null)
            .toList();
    if (dynamicTypes.isEmpty()) {
      return;
    }

    List<Appointment> legacyAppointments =
        appointmentRepository.findLegacyAppointmentsForPractitioner(sourcePractitionerId);
    if (legacyAppointments.isEmpty()) {
      return;
    }

    List<Appointment> dirtyAppointments = new ArrayList<>();
    for (Appointment appointment : legacyAppointments) {
      AppointmentType match = findBestDynamicType(appointment, dynamicTypes);
      if (match == null) {
        continue;
      }
      boolean refreshTitle = shouldRefreshLegacyTitle(appointment);
      appointment.setAppointmentType(match);
      if (refreshTitle) {
        appointment.setTitle(buildDynamicTitle(appointment, match));
      }
      dirtyAppointments.add(appointment);
    }

    if (!dirtyAppointments.isEmpty()) {
      appointmentRepository.saveAll(dirtyAppointments);
    }
  }

  private void refreshDynamicAppointments(Long sourcePractitionerId) {
    if (sourcePractitionerId == null) {
      return;
    }

    List<AppointmentType> dynamicTypes =
        repository.findBySourcePractitionerIdOrderByDisplayOrderAscIdAsc(sourcePractitionerId).stream()
            .filter(type -> type.getSourceActId() != null)
            .toList();
    if (dynamicTypes.isEmpty()) {
      return;
    }

    List<Appointment> appointments =
        appointmentRepository.findAppointmentsForPractitioner(sourcePractitionerId);
    if (appointments.isEmpty()) {
      return;
    }

    List<Appointment> dirtyAppointments = new ArrayList<>();
    for (Appointment appointment : appointments) {
      AppointmentType resolvedType = resolveDynamicTypeForAppointment(appointment, dynamicTypes);
      if (resolvedType == null) {
        continue;
      }

      boolean dirty = false;
      if (appointment.getAppointmentType() != resolvedType) {
        appointment.setAppointmentType(resolvedType);
        dirty = true;
      }

      if (shouldRefreshTitleForResolvedType(appointment, resolvedType)) {
        appointment.setTitle(buildDynamicTitle(appointment, resolvedType));
        dirty = true;
      }

      if (dirty) {
        dirtyAppointments.add(appointment);
      }
    }

    if (!dirtyAppointments.isEmpty()) {
      appointmentRepository.saveAll(dirtyAppointments);
    }
  }

  private AppointmentType findBestDynamicType(
      Appointment appointment, List<AppointmentType> dynamicTypes) {
    String visitReason = normalize(appointment.getVisitReasonCode());
    if (!visitReason.isEmpty()) {
      for (AppointmentType type : dynamicTypes) {
        if (visitReason.equals(normalize(type.getLabel()))) {
          return type;
        }
      }
    }

    List<AppointmentType> durationMatches =
        dynamicTypes.stream()
            .filter(
                type ->
                    type.getDefaultDurationMinutes() != null
                        && appointment.getDurationMinutes() != null
                        && type.getDefaultDurationMinutes().intValue()
                            == appointment.getDurationMinutes().intValue())
            .toList();
    if (durationMatches.size() == 1) {
      return durationMatches.get(0);
    }
    if (durationMatches.size() > 1) {
      List<AppointmentType> locationMatches = filterDurationMatchesByVisitMode(appointment, durationMatches);
      if (locationMatches.size() == 1) {
        return locationMatches.get(0);
      }
    }

    if (dynamicTypes.size() == 1) {
      return dynamicTypes.get(0);
    }

    return null;
  }

  private AppointmentType resolveDynamicTypeForAppointment(
      Appointment appointment, List<AppointmentType> dynamicTypes) {
    AppointmentType currentType = appointment.getAppointmentType();
    if (currentType != null
        && currentType.getSourceActId() != null
        && currentType.getSourcePractitionerId() != null) {
      return currentType;
    }
    return findBestDynamicType(appointment, dynamicTypes);
  }

  private boolean shouldRefreshLegacyTitle(Appointment appointment) {
    String currentTitle = normalize(appointment.getTitle());
    if (currentTitle.isEmpty()) {
      return true;
    }
    AppointmentType currentType = appointment.getAppointmentType();
    if (currentType == null) {
      return true;
    }
    String currentTypeLabel = normalize(currentType.getLabel());
    return !currentTypeLabel.isEmpty() && currentTitle.startsWith(currentTypeLabel);
  }

  private boolean shouldRefreshTitleForResolvedType(
      Appointment appointment, AppointmentType resolvedType) {
    String currentTitle = normalize(appointment.getTitle());
    if (currentTitle.isEmpty()) {
      return true;
    }

    String resolvedLabel = normalize(resolvedType.getLabel());
    if (!resolvedLabel.isEmpty() && currentTitle.startsWith(resolvedLabel)) {
      return false;
    }

    return isPatientOnlineBooking(appointment) || isLegacyGenericTitle(currentTitle);
  }

  private String buildDynamicTitle(Appointment appointment, AppointmentType type) {
    String label = type.getLabel() != null ? type.getLabel().trim() : "Acte";
    String existingTitle = appointment.getTitle() != null ? appointment.getTitle().trim() : "";
    String suffix = "";
    int emDashIndex = existingTitle.indexOf("—");
    if (emDashIndex >= 0 && emDashIndex + 1 < existingTitle.length()) {
      suffix = existingTitle.substring(emDashIndex + 1).trim();
    } else {
      suffix = joinPatientIdentity(appointment);
    }
    return suffix.isBlank() ? label : label + " — " + suffix;
  }

  private boolean isPatientOnlineBooking(Appointment appointment) {
    String description = normalize(appointment.getDescription());
    return description.contains("rendez vous reserve en ligne par le patient");
  }

  private boolean isLegacyGenericTitle(String currentTitle) {
    return currentTitle.startsWith("consultation")
        || currentTitle.startsWith("chirurgie")
        || currentTitle.startsWith("rendez vous")
        || currentTitle.startsWith("rdv");
  }

  private String joinPatientIdentity(Appointment appointment) {
    String prenom = appointment.getPatientPrenom() != null ? appointment.getPatientPrenom().trim() : "";
    String nom = appointment.getPatientNom() != null ? appointment.getPatientNom().trim() : "";
    return (prenom + " " + nom).trim();
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
    normalized = normalized.replaceAll("\\p{M}+", "");
    return normalized.toLowerCase();
  }

  private List<AppointmentType> filterDurationMatchesByVisitMode(
      Appointment appointment, List<AppointmentType> durationMatches) {
    String description = normalize(appointment.getDescription());
    boolean remoteAppointment =
        description.contains("teleconsultation")
            || description.contains("en ligne")
            || description.contains("video");
    boolean cabinetAppointment =
        description.contains("au cabinet")
            || description.contains("en clinique")
            || description.contains("prevu au cabinet")
            || description.contains("prevu en clinique");

    final boolean expectedRemote = !cabinetAppointment && remoteAppointment;

    return durationMatches.stream()
        .filter(type -> isRemoteType(type) == expectedRemote)
        .toList();
  }

  private boolean isRemoteType(AppointmentType type) {
    String label = normalize(type.getLabel());
    return label.contains("teleconsultation")
        || label.contains("video")
        || label.contains("en ligne")
        || label.contains("remote");
  }
}
