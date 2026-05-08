package com.medical.agenda.controller;

import com.medical.agenda.dto.DoctorDTO;
import com.medical.agenda.service.DoctorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

  private final DoctorService doctorService;

  public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @GetMapping
  public List<DoctorDTO> list() {
    return doctorService.findAll();
  }

  @PostMapping
  public DoctorDTO create(@RequestBody DoctorDTO body) {
    return doctorService.create(body);
  }

  /** Corps JSON : {@code name} obligatoire, {@code colorCode} optionnel (inchangé si absent ou vide). */
  @PutMapping("/{id}")
  public DoctorDTO update(@PathVariable Long id, @RequestBody DoctorDTO body) {
    return doctorService.update(id, body);
  }

  /** Upload multipart {@code file} — JPEG, PNG, WebP ou GIF ; met à jour {@code photo_url}. */
  @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DoctorDTO uploadPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return doctorService.savePhoto(id, file);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    doctorService.delete(id);
  }
}
