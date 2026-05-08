package com.medical.agenda.service;

import com.medical.agenda.dto.AppointmentTypeDTO;
import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.repository.AppointmentTypeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentTypeService {

  private final AppointmentTypeRepository repository;

  public AppointmentTypeService(AppointmentTypeRepository repository) {
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
    return toDto(repository.save(e));
  }

  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Type inconnu.");
    }
    repository.deleteById(id);
  }

  public static AppointmentTypeDTO toDto(AppointmentType e) {
    AppointmentTypeDTO dto = new AppointmentTypeDTO();
    dto.setId(e.getId());
    dto.setCode(e.getCode());
    dto.setLabel(e.getLabel());
    dto.setColorCode(e.getColorCode());
    dto.setDefaultDurationMinutes(e.getDefaultDurationMinutes());
    dto.setDisplayOrder(e.getDisplayOrder());
    dto.setActive(e.isActive());
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
}
