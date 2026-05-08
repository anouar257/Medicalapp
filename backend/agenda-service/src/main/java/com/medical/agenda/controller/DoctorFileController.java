package com.medical.agenda.controller;

import com.medical.agenda.service.DoctorPhotoFileService;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sert les portraits uploadés (fichiers présents sous {@code agenda.doctor-photos.directory}). */
@RestController
@RequestMapping("/api/doctor-files")
public class DoctorFileController {

  private final DoctorPhotoFileService doctorPhotoFileService;

  public DoctorFileController(DoctorPhotoFileService doctorPhotoFileService) {
    this.doctorPhotoFileService = doctorPhotoFileService;
  }

  @GetMapping("/{filename:.+}")
  public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
    Resource resource = doctorPhotoFileService.loadAsResource(filename);
    MediaType ct = doctorPhotoFileService.probeMediaType(filename);
    return ResponseEntity.ok()
        .contentType(ct)
        .cacheControl(CacheControl.noCache())
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(resource);
  }
}
