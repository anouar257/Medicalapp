package com.medical.agenda.controller;

import com.medical.agenda.dto.AppointmentDTO;
import com.medical.agenda.service.AppointmentService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
  public List<AppointmentDTO> list(
      @RequestParam("start") Instant rangeStart, @RequestParam("end") Instant rangeEnd) {
    return appointmentService.findInRange(rangeStart, rangeEnd);
  }

  @GetMapping("/{id}")
  public AppointmentDTO getById(@PathVariable Long id) {
    return appointmentService.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AppointmentDTO create(@RequestBody AppointmentDTO body) {
    return appointmentService.create(body);
  }

  @PutMapping("/{id}")
  public AppointmentDTO update(@PathVariable Long id, @RequestBody AppointmentDTO body) {
    return appointmentService.update(id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    appointmentService.delete(id);
  }
}
