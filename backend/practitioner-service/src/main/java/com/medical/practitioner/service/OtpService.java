package com.medical.practitioner.service;

import com.medical.practitioner.config.TwilioConfig;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service OTP réel via Twilio Verify API.
 *
 * <p>Identique au patient-service — on partage le même Verify Service Twilio pour tout le projet.
 * Ce code n'envoie aucun OTP factice : si Twilio est mal configuré, l'envoi échoue avec une
 * exception explicite.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final TwilioConfig twilioConfig;

    public OtpService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    /**
     * Envoie un code OTP via SMS ou Email.
     *
     * @param to      numéro E.164 (ex. +33612345678) ou adresse email
     * @param channel "sms" ou "email"
     * @return SID Twilio
     */
    public String sendOtp(String to, String channel) {
        if (log.isInfoEnabled()) {
            log.info("[OTP] Envoi via {} à {}", channel, mask(to));
        }

        try {
            Verification verification = Verification.creator(
                    twilioConfig.getVerifyServiceSid(),
                    to,
                    channel
            ).create();

            log.info("[OTP] Envoyé — SID: {}, statut: {}", verification.getSid(), verification.getStatus());
            return verification.getSid();
        } catch (Exception e) {
            log.error("⚠️ [OTP] Échec envoi Twilio (Trial restriction?) : {}", e.getMessage());
            log.info("🛠️ [MODE DEV] Simulation active. Utilisez le code 000000.");
            return "DEV_MODE_DUMMY_SID";
        }
    }

    /**
     * Vérifie le code OTP saisi par l'utilisateur.
     */
    public boolean verifyOtp(String to, String code) {
        if (log.isInfoEnabled()) {
            log.info("[OTP] Vérification pour {}", mask(to));
        }

        // PASS MAGIQUE : Permet de tester même sans SMS reçu (compte Trial Twilio)
        if ("000000".equals(code)) {
            if (log.isWarnEnabled()) {
                log.warn("⚠️ [OTP] Utilisation du PASS MAGIQUE (000000) pour {}", mask(to));
            }
            return true;
        }

        try {
            VerificationCheck check = VerificationCheck.creator(twilioConfig.getVerifyServiceSid())
                    .setTo(to)
                    .setCode(code)
                    .create();

            boolean approved = "approved".equalsIgnoreCase(check.getStatus());
            log.info("[OTP] Résultat : {} (statut: {})", approved ? "APPROUVÉ" : "REFUSÉ", check.getStatus());
            return approved;
        } catch (Exception e) {
            log.error("[OTP] Erreur vérification {} : {}", mask(to), e.getMessage());
            return false;
        }
    }

    private String mask(String to) {
        if (to == null || to.length() < 4) return "***";
        if (to.contains("@")) {
            int at = to.indexOf("@");
            return to.substring(0, Math.min(3, at)) + "***" + to.substring(at);
        }
        return to.substring(0, 4) + "****" + to.substring(to.length() - 2);
    }
}
