package com.medical.agenda.service;

import com.medical.agenda.dto.GlobalStatsResponseDTO;
import com.medical.agenda.dto.PlatformSummaryRemoteDTO;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.DoctorRepository;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GlobalStatsService {

  private static final Logger log = LoggerFactory.getLogger(GlobalStatsService.class);

  private final DoctorRepository doctorRepository;
  private final AppointmentRepository appointmentRepository;
  private final RestTemplate http;

  @Value("${integration.practitioner-base-url:http://localhost:8083}")
  private String practitionerBaseUrl;

  public GlobalStatsService(
      DoctorRepository doctorRepository,
      AppointmentRepository appointmentRepository,
      RestTemplateBuilder restTemplateBuilder) {
    this.doctorRepository = doctorRepository;
    this.appointmentRepository = appointmentRepository;
    this.http =
        restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(10))
            .build();
  }

  public GlobalStatsResponseDTO build(String authorizationHeader) {
    PlatformSummaryRemoteDTO remote = fetchPlatformSummary(authorizationHeader);

    GlobalStatsResponseDTO out = new GlobalStatsResponseDTO();
    if (remote != null) {
      out.setTotalActiveCabinets(remote.getTotalActiveCabinets());
      out.setTotalPractitionerProfiles(remote.getTotalPractitionerProfiles());
      out.setTotalPatients(remote.getTotalPatients());
    }
    out.setTotalDoctorsInAgenda(doctorRepository.count());
    out.setTotalSyncedDoctorsInAgenda(doctorRepository.countByExternalPractitionerIdIsNotNull());
    out.setTotalAppointments(appointmentRepository.count());
    return out;
  }

  private PlatformSummaryRemoteDTO fetchPlatformSummary(String authorizationHeader) {
    try {
      HttpHeaders headers = new HttpHeaders();
      if (authorizationHeader != null && !authorizationHeader.isBlank()) {
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader.trim());
      }
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      String base = practitionerBaseUrl.replaceAll("/$", "");
      return http
          .exchange(
              base + "/api/pro/platform/stats/platform-summary",
              HttpMethod.GET,
              entity,
              PlatformSummaryRemoteDTO.class)
          .getBody();
    } catch (RestClientException e) {
      log.warn("[GlobalStats] practitioner-service indisponible : {}", e.getMessage());
      return null;
    }
  }
}
