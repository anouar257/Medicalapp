package com.medical.practitioner.service;

import com.medical.practitioner.dto.PlatformSummaryDTO;
import com.medical.practitioner.repository.MedicalOrganizationRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlatformStatsService {

  private static final Logger log = LoggerFactory.getLogger(PlatformStatsService.class);
  private static final String X_PLATFORM_STATS_KEY = "X-Platform-Stats-Key";

  private final MedicalOrganizationRepository organizationRepository;
  private final PractitionerProfileRepository practitionerProfileRepository;
  private final RestTemplate http;

  @Value("${integration.patient-base-url:http://localhost:8082}")
  private String patientBaseUrl;

  @Value("${integration.platform-stats-secret:dev-platform-stats-key-change-me}")
  private String platformStatsSecret;

  public PlatformStatsService(
      MedicalOrganizationRepository organizationRepository,
      PractitionerProfileRepository practitionerProfileRepository,
      RestTemplateBuilder restTemplateBuilder) {
    this.organizationRepository = organizationRepository;
    this.practitionerProfileRepository = practitionerProfileRepository;
    this.http =
        restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(8))
            .build();
  }

  public PlatformSummaryDTO buildPlatformSummary() {
    long cabinets = organizationRepository.countByActifTrue();
    long practitioners = practitionerProfileRepository.count();
    long patients = fetchPatientCount();
    PlatformSummaryDTO dto = new PlatformSummaryDTO();
    dto.setTotalActiveCabinets(cabinets);
    dto.setTotalPractitionerProfiles(practitioners);
    dto.setTotalPatients(patients);
    return dto;
  }

  private long fetchPatientCount() {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(X_PLATFORM_STATS_KEY, platformStatsSecret);
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      String base = patientBaseUrl.replaceAll("/$", "");
      ResponseEntity<Map<String, Object>> resp =
          http.exchange(
              base + "/api/internal/stats/patient-count",
              HttpMethod.GET,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});
      Map<String, Object> body = resp.getBody();
      if (body == null) {
        return 0;
      }
      Object n = body.get("totalPatients");
      if (n instanceof Number number) {
        return number.longValue();
      }
      return 0;
    } catch (RestClientException e) {
      log.warn("[PlatformStats] patient-service indisponible — totalPatients=0 : {}", e.getMessage());
      return 0;
    }
  }

  public List<Map<String, Object>> listPlatformPatients() {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(X_PLATFORM_STATS_KEY, platformStatsSecret);
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      String base = patientBaseUrl.replaceAll("/$", "");
      ResponseEntity<List<Map<String, Object>>> resp =
          http.exchange(
              base + "/api/internal/stats/patients",
              HttpMethod.GET,
              entity,
              new ParameterizedTypeReference<List<Map<String, Object>>>() {});
      return resp.getBody() != null ? resp.getBody() : List.of();
    } catch (RestClientException e) {
      log.warn("[PlatformStats] Impossible de lister les patients : {}", e.getMessage());
      return List.of();
    }
  }

  public void togglePatientActive(Long patientId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(X_PLATFORM_STATS_KEY, platformStatsSecret);
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      String base = patientBaseUrl.replaceAll("/$", "");
      http.exchange(
          base + "/api/internal/stats/patients/" + patientId + "/toggle-active",
          HttpMethod.PUT,
          entity,
          Void.class);
    } catch (RestClientException e) {
      log.warn("[PlatformStats] Impossible de changer le statut du patient {} : {}", patientId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service patient injoignable");
    }
  }
}
