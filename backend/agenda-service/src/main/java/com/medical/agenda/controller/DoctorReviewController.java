package com.medical.agenda.controller;

import com.medical.agenda.dto.DoctorReviewDTO;
import com.medical.agenda.dto.DoctorReviewRequestDTO;
import com.medical.agenda.service.DoctorReviewService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class DoctorReviewController {

  private final DoctorReviewService reviewService;

  public DoctorReviewController(DoctorReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('PATIENT')")
  public DoctorReviewDTO create(
      Authentication authentication, @RequestBody DoctorReviewRequestDTO request) {
    return reviewService.create(authentication, request);
  }

  @GetMapping("/practitioner/{externalPractitionerId}")
  public List<DoctorReviewDTO> listForPractitioner(@PathVariable Long externalPractitionerId) {
    return reviewService.listForPractitioner(externalPractitionerId);
  }
}
