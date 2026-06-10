package com.medical.practitioner.service;

import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.Specialty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Synchronisation des praticiens vers l'agenda-service.
 *
 * <p>Lorsqu'un {@link PractitionerProfile} est créé ou mis à jour, on pousse les
 * informations d'affichage (nom complet, couleur, photo, spécialité) vers le
 * doctor de l'agenda-service via un endpoint dédié {@code POST /api/doctors/sync}.
 * Cela permet à l'agenda d'afficher le praticien dans le calendrier sans appel
 * inter-services à chaque requête.
 *
 * <p>L'opération est <strong>best-effort</strong> : un échec de synchronisation
 * (agenda-service indisponible) est loggué mais n'empêche pas la création du
 * praticien dans le practitioner-service.
 */
@Service
public class AgendaSyncService {

    private static final Logger log = LoggerFactory.getLogger(AgendaSyncService.class);

    private final RestTemplate restTemplate;
    private final String agendaBaseUrl;
    private final boolean syncEnabled;
    private final String agendaSyncSecret;

    public AgendaSyncService(RestTemplate practitionerRestTemplate,
                             @Value("${agenda.base-url:http://localhost:8081}") String agendaBaseUrl,
                             @Value("${agenda.sync-enabled:true}") boolean syncEnabled,
                             @Value("${agenda.sync-secret:}") String agendaSyncSecret) {
        this.restTemplate = practitionerRestTemplate;
        this.agendaBaseUrl = agendaBaseUrl;
        this.syncEnabled = syncEnabled;
        this.agendaSyncSecret = agendaSyncSecret;
    }

    /**
     * Pousse le praticien vers l'agenda-service. Crée ou met à jour le {@code Doctor}
     * dont {@code externalPractitionerId} = {@code profile.id}.
     */
    public void syncPractitioner(PractitionerProfile profile) {
        if (!syncEnabled) {
            log.debug("[AgendaSync] Synchronisation désactivée — skip");
            return;
        }
        if (profile == null || profile.getId() == null) {
            log.warn("[AgendaSync] Profil invalide — skip");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("externalPractitionerId", profile.getId());
            payload.put("name", buildFullName(profile));
            payload.put("colorCode", profile.getColorCode() != null ? profile.getColorCode() : "#0ea5e9");
            payload.put("photoUrl", profile.getPhotoUrl());
            payload.put("specialty", buildSpecialtyLabel(profile));
            payload.put("specialtyCode", buildPrimarySpecialtyCode(profile));
            if (profile.getProUser() != null && profile.getProUser().getOrganization() != null) {
                payload.put("organizationId", profile.getProUser().getOrganization().getId());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (agendaSyncSecret != null && !agendaSyncSecret.isBlank()) {
                headers.set("X-Agenda-Sync-Secret", agendaSyncSecret);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String url = agendaBaseUrl + "/api/doctors/sync";
            restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            log.info("[AgendaSync] Praticien {} synchronisé vers l'agenda-service", profile.getId());
        } catch (Exception e) {
            log.warn("[AgendaSync] Échec synchronisation praticien {} : {}", profile.getId(), e.getMessage());
        }
    }

    /**
     * Supprime la synchronisation côté agenda-service (best-effort).
     */
    public void unsyncPractitioner(Long practitionerId) {
        if (!syncEnabled || practitionerId == null) return;
        try {
            String url = agendaBaseUrl + "/api/doctors/sync/" + practitionerId;
            HttpHeaders headers = new HttpHeaders();
            if (agendaSyncSecret != null && !agendaSyncSecret.isBlank()) {
                headers.set("X-Agenda-Sync-Secret", agendaSyncSecret);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.info("[AgendaSync] Praticien {} désynchronisé", practitionerId);
        } catch (Exception e) {
            log.warn("[AgendaSync] Échec désynchronisation praticien {} : {}", practitionerId, e.getMessage());
        }
    }

    private String resolveTitre(PractitionerProfile p) {
        if (p.getTitre() == null) {
            return "";
        } else if ("DR".equals(p.getTitre().name())) {
            return "Dr. ";
        } else if ("PR".equals(p.getTitre().name())) {
            return "Pr. ";
        }
        return "";
    }

    private String buildFullName(PractitionerProfile p) {
        String prenom = p.getProUser() != null && p.getProUser().getPrenom() != null
                ? p.getProUser().getPrenom() : "";
        String nom = p.getProUser() != null && p.getProUser().getNom() != null
                ? p.getProUser().getNom() : "";
        String titre = resolveTitre(p);
        return (titre + prenom + " " + nom).trim();
    }

    private String buildSpecialtyLabel(PractitionerProfile p) {
        if (p.getSpecialites() == null || p.getSpecialites().isEmpty()) {
            return "Médecine générale";
        }
        return p.getSpecialites().stream()
                .map(Specialty::getLibelle)
                .filter(lib -> lib != null && !lib.isBlank())
                .reduce((a, b) -> a + " / " + b)
                .orElse("Médecine générale");
    }

    private String buildPrimarySpecialtyCode(PractitionerProfile p) {
        if (p.getSpecialites() == null || p.getSpecialites().isEmpty()) {
            return "MEDECINE_GENERALE";
        }
        Specialty s = p.getSpecialites().iterator().next();
        if (s.getCode() != null && !s.getCode().isBlank()) {
            return s.getCode().trim().toUpperCase();
        }
        return "MEDECINE_GENERALE";
    }

    @Configuration
    public static class RestTemplateConfig {
        @Bean
        public RestTemplate practitionerRestTemplate(RestTemplateBuilder builder) {
            return builder
                    .connectTimeout(Duration.ofSeconds(3))
                    .readTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }
}
