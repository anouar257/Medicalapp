package com.medical.agenda.controller;

import com.medical.agenda.dto.AppointmentTypeDTO;
import com.medical.agenda.service.AppointmentTypeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointment-types")
public class AppointmentTypeController {

  private final AppointmentTypeService service;

  public AppointmentTypeController(AppointmentTypeService service) {
    this.service = service;
  }

  @GetMapping
  public List<AppointmentTypeDTO> list() {
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AppointmentTypeDTO create(@RequestBody AppointmentTypeDTO body) {
    return service.create(body);
  }

  @PutMapping("/{id}")
  public AppointmentTypeDTO update(@PathVariable Long id, @RequestBody AppointmentTypeDTO body) {
    return service.update(id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
