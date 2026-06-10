package com.medical.agenda.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CabinetHoursClient {

  private static final Logger log = LoggerFactory.getLogger(CabinetHoursClient.class);
  private final RestTemplate http;

  @Value("${integration.practitioner-base-url:http://localhost:8083}")
  private String practitionerBaseUrl;

  public CabinetHoursClient(RestTemplateBuilder restTemplateBuilder) {
    this.http =
        restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(10))
            .build();
  }

  public static class CabinetHoraireDTO {
    private String jour;
    private String heureDebut;
    private String heureFin;
    private boolean continu;

    public String getJour() {
      return jour;
    }

    public void setJour(String jour) {
      this.jour = jour;
    }

    public String getHeureDebut() {
      return heureDebut;
    }

    public void setHeureDebut(String heureDebut) {
      this.heureDebut = heureDebut;
    }

    public String getHeureFin() {
      return heureFin;
    }

    public void setHeureFin(String heureFin) {
      this.heureFin = heureFin;
    }

    public boolean isContinu() {
      return continu;
    }

    public void setContinu(boolean continu) {
      this.continu = continu;
    }
  }

  public List<CabinetHoraireDTO> getCabinetHoraires(Long organizationId) {
    if (organizationId == null) {
      return Collections.emptyList();
    }
    try {
      String base = practitionerBaseUrl.replaceAll("/$", "");
      String url = base + "/api/pro/public/cabinets/" + organizationId + "/horaires";
      
      return http.exchange(
          url,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<List<CabinetHoraireDTO>>() {}
      ).getBody();
    } catch (RestClientException e) {
      log.warn("[CabinetHoursClient] Impossible de récupérer les horaires pour le cabinet {}: {}", organizationId, e.getMessage());
      return Collections.emptyList();
    }
  }
}
