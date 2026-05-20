package com.medical.agenda.controller;

import com.medical.agenda.dto.AppointmentTypePublicDTO;
import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.repository.AppointmentTypeRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointment-types")
public class AppointmentTypeController {

  private final AppointmentTypeRepository repository;

  public AppointmentTypeController(AppointmentTypeRepository repository) {
    this.repository = repository;
  }

  /** Types de visite actifs — utilisés par le parcours patient (durée par défaut, code). */
  @GetMapping
  @PreAuthorize("hasAnyRole('PATIENT','ASSISTANT','PRATICIEN','ADMIN')")
  public List<AppointmentTypePublicDTO> listActive() {
    return repository.findByActiveTrueOrderByDisplayOrderAsc().stream()
        .map(AppointmentTypeController::toPublic)
        .toList();
  }

  private static AppointmentTypePublicDTO toPublic(AppointmentType t) {
    AppointmentTypePublicDTO d = new AppointmentTypePublicDTO();
    d.setId(t.getId());
    d.setCode(t.getCode());
    d.setLabel(t.getLabel());
    d.setColorCode(t.getColorCode());
    d.setDefaultDurationMinutes(t.getDefaultDurationMinutes());
    d.setDisplayOrder(t.getDisplayOrder());
    d.setActive(t.isActive());
    return d;
  }
}
