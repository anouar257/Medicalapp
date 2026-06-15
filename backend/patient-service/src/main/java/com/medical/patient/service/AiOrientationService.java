package com.medical.patient.service;

import com.medical.patient.dto.AiOrientationRequest;
import com.medical.patient.dto.AiOrientationResponse;
import com.medical.patient.dto.AiServiceRequest;
import com.medical.patient.dto.SpecialtyDTO;
import com.medical.patient.dto.SpecialtyRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Service
public class AiOrientationService {

    private static final Logger log = LoggerFactory.getLogger(AiOrientationService.class);

    private final RestTemplate restTemplate;

    @Value("${ai-service.url:http://ai-service:8001}")
    private String aiServiceUrl;

    @Value("${integration.practitioner-service-url:http://practitioner-service:8083}")
    private String practitionerServiceUrl;

    // Cache management variables
    private List<SpecialtyDTO> cachedSpecialties = null;
    private long lastCacheFetchTime = 0;
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes TTL

    // Robust static fallback list in case practitioner-service is starting up or down
    private static final List<SpecialtyDTO> FALLBACK_SPECIALTIES = List.of(
        new SpecialtyDTO(1L, "MEDECINE_GENERALE", "Médecine générale"),
        new SpecialtyDTO(2L, "CARDIOLOGIE", "Cardiologie"),
        new SpecialtyDTO(3L, "DERMATOLOGIE", "Dermatologie"),
        new SpecialtyDTO(4L, "PEDIATRIE", "Pédiatrie"),
        new SpecialtyDTO(5L, "GYNECOLOGIE", "Gynécologie"),
        new SpecialtyDTO(6L, "OPHTALMOLOGIE", "Ophtalmologie"),
        new SpecialtyDTO(7L, "ORL", "ORL (Oto-rhino-laryngologie)"),
        new SpecialtyDTO(8L, "PSYCHIATRIE", "Psychiatrie"),
        new SpecialtyDTO(9L, "RADIOLOGIE", "Radiologie"),
        new SpecialtyDTO(10L, "NEUROLOGIE", "Neurologie"),
        new SpecialtyDTO(11L, "RHUMATOLOGIE", "Rhumatologie"),
        new SpecialtyDTO(12L, "UROLOGIE", "Urologie"),
        new SpecialtyDTO(13L, "ENDOCRINOLOGIE", "Endocrinologie"),
        new SpecialtyDTO(14L, "GASTROENTEROLOGIE", "Gastro-entérologie"),
        new SpecialtyDTO(15L, "PNEUMOLOGIE", "Pneumologie"),
        new SpecialtyDTO(16L, "CHIRURGIE_GENERALE", "Chirurgie générale"),
        new SpecialtyDTO(17L, "CHIRURGIE_DENTAIRE", "Chirurgie dentaire"),
        new SpecialtyDTO(18L, "ORTHODONTIE", "Orthodontie"),
        new SpecialtyDTO(19L, "KINESITHERAPIE", "Kinésithérapie"),
        new SpecialtyDTO(20L, "OSTEOPATHIE", "Ostéopathie"),
        new SpecialtyDTO(21L, "SAGE_FEMME", "Sage-femme"),
        new SpecialtyDTO(22L, "INFIRMIER", "Infirmier(ère)"),
        new SpecialtyDTO(23L, "DEFAULT", "Générique (questionnaires par défaut)"),
        new SpecialtyDTO(24L, "AUDIOPROTHESE", "Audioprothèse")
    );

    public AiOrientationService(RestTemplateBuilder restTemplateBuilder) {
        // Enforce short timeouts (2s connect, 3s read)
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(2000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
    }

    /**
     * Gets or updates cached specialties list from practitioner-service.
     */
    private synchronized List<SpecialtyDTO> getSpecialtiesCatalog() {
        long now = System.currentTimeMillis();
        if (cachedSpecialties != null && (now - lastCacheFetchTime < CACHE_TTL_MS)) {
            return cachedSpecialties;
        }

        String url = practitionerServiceUrl + "/api/pro/specialties/public/list";
        try {
            log.info("Fetching specialties catalog from: {}", url);
            SpecialtyDTO[] response = restTemplate.getForObject(url, SpecialtyDTO[].class);
            if (response != null && response.length > 0) {
                cachedSpecialties = List.of(response);
                lastCacheFetchTime = now;
                log.info("Successfully fetched and cached {} specialties", cachedSpecialties.size());
                return cachedSpecialties;
            }
        } catch (Exception e) {
            log.error("Failed to fetch specialties from practitioner-service: {} - using fallback config", e.getMessage());
        }

        // Return fallback if we have nothing cached yet
        if (cachedSpecialties == null) {
            return FALLBACK_SPECIALTIES;
        }
        return cachedSpecialties;
    }

    public AiOrientationResponse getOrientation(AiOrientationRequest request) {
        String url = aiServiceUrl + "/api/ai/orientation";
        try {
            List<SpecialtyDTO> specialtiesCatalog = getSpecialtiesCatalog();

            // Construct payload with specialties catalog for the Python service
            AiServiceRequest aiRequest = new AiServiceRequest(
                request.message(),
                request.language(),
                request.history(),
                specialtiesCatalog
            );

            log.info("Contacting AI service at: {} for language: {} with catalog of {} specialties", 
                url, request.language(), specialtiesCatalog.size());
                
            ResponseEntity<AiOrientationResponse> response = restTemplate.postForEntity(url, aiRequest, AiOrientationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("AI service call failed: {} - returning fallback response", e.getMessage());
        }

        return getFallbackResponse(request.language());
    }

    private AiOrientationResponse getFallbackResponse(String lang) {
        String message;
        String warning;
        List<SpecialtyRecommendation> specialties;

        String cleanLang = lang != null ? lang.toLowerCase() : "fr";
        if ("ar".equals(cleanLang)) {
            message = "لا يمكنني تقديم تشخيص طبي، ولكن يمكنني توجيهكم.";
            warning = "هذا المساعد لا يغني عن الاستشارة الطبية. في حالة الطوارئ، يرجى الاتصال بالإسعاف فوراً.";
            specialties = List.of(new SpecialtyRecommendation(1L, "MEDECINE_GENERALE", "طبيب عام"));
        } else if ("en".equals(cleanLang)) {
            message = "I cannot make a medical diagnosis, but I can guide you to the right specialist.";
            warning = "This assistant does not replace a medical consultation. In case of emergency, contact emergency services (911).";
            specialties = List.of(new SpecialtyRecommendation(1L, "MEDECINE_GENERALE", "General Practitioner"));
        } else {
            message = "Je ne peux pas poser de diagnostic, mais je peux vous orienter.";
            warning = "Cet assistant ne remplace pas une consultation médicale. En cas d'urgence, contactez le 15 ou rendez-vous aux urgences.";
            specialties = List.of(new SpecialtyRecommendation(1L, "MEDECINE_GENERALE", "Médecin généraliste"));
        }

        return new AiOrientationResponse(message, specialties, "normal", warning, false);
    }
}
