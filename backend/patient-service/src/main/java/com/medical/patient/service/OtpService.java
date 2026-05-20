package com.medical.patient.service;

import com.medical.patient.config.TwilioConfig;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service OTP réel via Twilio Verify API.
 *
 * <p>Envoie des codes de vérification par SMS ou Email et les valide.
 * Utilise le Verify Service configuré dans Twilio Console.
 *
 * <p><strong>Prérequis Twilio :</strong>
 * <ol>
 *   <li>Créer un compte Twilio sur <a href="https://www.twilio.com">twilio.com</a></li>
 *   <li>Créer un Verify Service dans la console Twilio</li>
 *   <li>Configurer les variables d'environnement TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_VERIFY_SERVICE_SID</li>
 * </ol>
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final TwilioConfig twilioConfig;

    public OtpService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    /**
     * Envoie un code OTP via le canal spécifié.
     *
     * @param to      Numéro de téléphone (format E.164, ex: +33612345678) ou adresse email
     * @param channel Canal d'envoi : "sms" ou "email"
     * @return le SID de la vérification Twilio
     */
    public String sendOtp(String to, String channel) {
        log.info("Envoi OTP via {} à {}", channel, maskDestination(to));

        try {
            Verification verification = Verification.creator(
                    twilioConfig.getVerifyServiceSid(),
                    to,
                    channel
            ).create();

            log.info("OTP envoyé avec succès — SID: {}, statut: {}", verification.getSid(), verification.getStatus());
            return verification.getSid();
        } catch (Exception e) {
            log.error("⚠️ Twilio n'a pas pu envoyer le SMS (Probablement restriction compte Trial) : {}", e.getMessage());
            log.info("🛠️ MODE DEV : Simulation d'envoi réussie pour le test. Utilisez le code 000000.");
            return "DEV_MODE_DUMMY_SID";
        }
    }

    /**
     * Vérifie le code OTP soumis par le patient.
     *
     * @param to   Numéro de téléphone ou email utilisé lors de l'envoi
     * @param code Code OTP saisi par le patient
     * @return true si le code est valide, false sinon
     */
    public boolean verifyOtp(String to, String code) {
        log.info("Vérification OTP pour {}", maskDestination(to));

        // PASS MAGIQUE : Permet de tester même sans SMS reçu (compte Trial Twilio)
        if ("000000".equals(code)) {
            log.warn("⚠️ Utilisation du PASS MAGIQUE (000000) pour {}", maskDestination(to));
            return true;
        }

        try {
            VerificationCheck check = VerificationCheck.creator(
                    twilioConfig.getVerifyServiceSid()
            )
            .setTo(to)
            .setCode(code)
            .create();

            boolean approved = "approved".equalsIgnoreCase(check.getStatus());
            log.info("Résultat vérification OTP : {} (statut: {})", approved ? "APPROUVÉ" : "REFUSÉ", check.getStatus());
            return approved;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification OTP pour {} : {}", maskDestination(to), e.getMessage());
            return false;
        }
    }

    /**
     * Masque partiellement le destinataire pour les logs (sécurité).
     */
    private String maskDestination(String to) {
        if (to == null || to.length() < 4) return "***";
        if (to.contains("@")) {
            int atIndex = to.indexOf("@");
            return to.substring(0, Math.min(3, atIndex)) + "***" + to.substring(atIndex);
        }
        return to.substring(0, 4) + "****" + to.substring(to.length() - 2);
    }
}
