package com.medical.agenda.service;

import com.medical.agenda.dto.DoctorReviewDTO;
import com.medical.agenda.dto.DoctorReviewRequestDTO;
import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import com.medical.agenda.entity.DoctorReview;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.DoctorReviewRepository;
import com.medical.agenda.security.AgendaPatientPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

@Service
public class DoctorReviewService {

  private final AppointmentRepository appointmentRepository;
  private final DoctorReviewRepository reviewRepository;
  private final RestTemplate restTemplate;

  @Value("${integration.practitioner-base-url:http://localhost:8083}")
  private String practitionerBaseUrl;

  @Value("${integration.agenda-sync-secret:}")
  private String agendaSyncSecret;

  public DoctorReviewService(
      AppointmentRepository appointmentRepository,
      DoctorReviewRepository reviewRepository,
      RestTemplateBuilder restTemplateBuilder) {
    this.appointmentRepository = appointmentRepository;
    this.reviewRepository = reviewRepository;
    this.restTemplate = restTemplateBuilder.build();
  }

  public DoctorReviewDTO create(Authentication authentication, DoctorReviewRequestDTO request) {
    AgendaPatientPrincipal patient = (AgendaPatientPrincipal) authentication.getPrincipal();
    if (request == null || request.getAppointmentId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appointmentId requis");
    }

    Appointment appointment =
        appointmentRepository
            .findById(request.getAppointmentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RDV introuvable"));

    if (appointment.getPatientId() == null || !appointment.getPatientId().equals(patient.patientId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce rendez-vous ne vous appartient pas");
    }
    if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Un avis est possible uniquement après un rendez-vous terminé");
    }
    if (appointment.getDoctor() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Ce rendez-vous n'est rattaché à aucun médecin");
    }
    if (appointment.getDoctor().getExternalPractitionerId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Ce médecin n'est pas lié à un profil public");
    }
    if (reviewRepository.existsByAppointmentId(appointment.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Un avis existe déjà pour ce rendez-vous");
    }

    DoctorReview review = new DoctorReview();
    review.setAppointment(appointment);
    review.setPatientId(patient.patientId());
    review.setPatientDisplayName(resolvePatientDisplayName(appointment));
    review.setDoctorId(appointment.getDoctor().getId());
    review.setExternalPractitionerId(appointment.getDoctor().getExternalPractitionerId());
    review.setRating(normalizeRating(request.getRating()));
    review.setPunctualityRating(normalizeRating(request.getPunctualityRating()));
    review.setComment(normalizeComment(request.getComment()));
    review.setCreatedAt(Instant.now());

    DoctorReview saved = reviewRepository.save(review);
    syncPractitionerSummary(saved.getExternalPractitionerId());
    return toDto(saved);
  }

  public List<DoctorReviewDTO> listForPractitioner(Long externalPractitionerId) {
    if (externalPractitionerId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalPractitionerId requis");
    }
    return reviewRepository.findByExternalPractitionerIdOrderByCreatedAtDesc(externalPractitionerId).stream()
        .map(this::toDto)
        .toList();
  }

  private Integer normalizeRating(Integer value) {
    if (value == null || value < 1 || value > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note doit être entre 1 et 5");
    }
    return value;
  }

  private String normalizeComment(String comment) {
    if (comment == null) return "";
    String trimmed = comment.trim();
    return trimmed.length() > 1200 ? trimmed.substring(0, 1200) : trimmed;
  }

  private String resolvePatientDisplayName(Appointment appointment) {
    String name =
        String.join(
                " ",
                appointment.getPatientPrenom() == null ? "" : appointment.getPatientPrenom().trim(),
                appointment.getPatientNom() == null ? "" : appointment.getPatientNom().trim())
            .trim();
    if (name.isBlank()) {
      return "Patient";
    }
    String[] parts = name.split("\\s+");
    if (parts.length == 1) {
      return parts[0];
    }
    return parts[0] + " " + parts[1].charAt(0) + ".";
  }

  private DoctorReviewDTO toDto(DoctorReview review) {
    DoctorReviewDTO dto = new DoctorReviewDTO();
    dto.setId(review.getId());
    dto.setAppointmentId(review.getAppointment().getId());
    dto.setExternalPractitionerId(review.getExternalPractitionerId());
    dto.setPatientDisplayName(review.getPatientDisplayName());
    dto.setRating(review.getRating());
    dto.setPunctualityRating(review.getPunctualityRating());
    dto.setComment(review.getComment());
    dto.setCreatedAt(review.getCreatedAt());
    return dto;
  }

  private void syncPractitionerSummary(Long externalPractitionerId) {
    if (externalPractitionerId == null) {
      return;
    }
    List<DoctorReview> reviews =
        reviewRepository.findByExternalPractitionerIdOrderByCreatedAtDesc(externalPractitionerId);
    if (reviews.isEmpty()) {
      return;
    }
    double avg =
        reviews.stream()
            .mapToInt(r -> r.getRating() == null ? 0 : r.getRating())
            .average()
            .orElse(0.0);
    int count = reviews.size();

    try {
      String url = practitionerBaseUrl.replaceAll("/$", "") + "/api/internal/reviews/summary";
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (agendaSyncSecret != null && !agendaSyncSecret.isBlank()) {
        headers.set("X-Agenda-Sync-Secret", agendaSyncSecret);
      }
      java.util.Map<String, Object> body = new java.util.HashMap<>();
      body.put("externalPractitionerId", externalPractitionerId);
      body.put("globalRating", BigDecimal.valueOf(avg).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue());
      body.put("reviewCount", count);
      restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    } catch (Exception ignored) {
      // Best effort: l'UI reste fonctionnelle même si la synchro inter-service échoue.
    }
  }
}
