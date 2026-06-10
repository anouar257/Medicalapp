package com.medical.practitioner.service;

import com.medical.practitioner.dto.PractitionerActDTO;
import com.medical.practitioner.entity.PractitionerAct;
import com.medical.practitioner.entity.PractitionerProfile;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Synchronisation des actes mÃ©dicaux vers l'agenda-service.
 *
 * <p>Chaque acte du profil praticien devient un type de visite synchronisÃ© afin que l'agenda
 * cabinet et le parcours patient affichent les mÃªmes libellÃ©s, durÃ©es et tarifs.
 */
@Service
public class AgendaActSyncService {

    private static final Logger log = LoggerFactory.getLogger(AgendaActSyncService.class);

    private final RestTemplate restTemplate;
    private final String agendaBaseUrl;
    private final boolean syncEnabled;
    private final String agendaSyncSecret;

    public AgendaActSyncService(RestTemplate practitionerRestTemplate,
                                @Value("${agenda.base-url}") String agendaBaseUrl,
                                @Value("${agenda.sync-enabled}") boolean syncEnabled,
                                @Value("${agenda.sync-secret:}") String agendaSyncSecret) {
        this.restTemplate = practitionerRestTemplate;
        this.agendaBaseUrl = agendaBaseUrl;
        this.syncEnabled = syncEnabled;
        this.agendaSyncSecret = agendaSyncSecret;
    }

    public void syncAct(PractitionerAct act) {
        if (!syncEnabled || act == null || act.getId() == null || act.getPractitioner() == null) {
            return;
        }
        PractitionerProfile profile = act.getPractitioner();
        Long practitionerId = profile.getId();
        if (practitionerId == null) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("code", PractitionerActDTO.buildAgendaTypeCode(practitionerId, act.getId()));
            payload.put("label", act.getName());
            payload.put("colorCode", resolveColor(profile));
            payload.put("defaultDurationMinutes", act.getDurationMinutes());
            payload.put("displayOrder", safeDisplayOrder(act));
            payload.put("active", true);
            payload.put("price", act.getPrice());
            payload.put("priceVariable", act.isPriceVariable());
            payload.put("sourcePractitionerId", practitionerId);
            payload.put("sourceActId", act.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (agendaSyncSecret != null && !agendaSyncSecret.isBlank()) {
                headers.set("X-Agenda-Sync-Secret", agendaSyncSecret);
            }
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = agendaBaseUrl + "/api/appointment-types/sync";
            restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            log.info("[AgendaActSync] Acte {} synchronisé vers l'agenda-service", act.getId());
        } catch (Exception e) {
            log.warn("[AgendaActSync] Échec synchronisation acte {} : {}", act.getId(), e.getMessage());
        }
    }

    public void deactivateAct(Long practitionerId, Long actId) {
        if (!syncEnabled || practitionerId == null || actId == null) {
            return;
        }
        try {
            String code = PractitionerActDTO.buildAgendaTypeCode(practitionerId, actId);
            String url = agendaBaseUrl + "/api/appointment-types/sync/" + code;
            HttpHeaders headers = new HttpHeaders();
            if (agendaSyncSecret != null && !agendaSyncSecret.isBlank()) {
                headers.set("X-Agenda-Sync-Secret", agendaSyncSecret);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.info("[AgendaActSync] Acte {} désactivé côté agenda-service", actId);
        } catch (Exception e) {
            log.warn("[AgendaActSync] Échec désactivation acte {} : {}", actId, e.getMessage());
        }
    }

    private String resolveColor(PractitionerProfile profile) {
        if (profile.getColorCode() != null && !profile.getColorCode().isBlank()) {
            return profile.getColorCode().trim();
        }
        return "#0ea5e9";
    }

    private int safeDisplayOrder(PractitionerAct act) {
        Long id = act.getId();
        if (id == null) {
            return 100;
        }
        return Math.toIntExact(Math.min(id, Integer.MAX_VALUE));
    }
}
