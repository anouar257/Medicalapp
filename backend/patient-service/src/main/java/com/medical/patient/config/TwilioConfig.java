package com.medical.patient.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialisation du SDK Twilio au démarrage de l'application.
 * Utilise les identifiants configurés dans application.yml (variables d'environnement en production).
 */
@Configuration
public class TwilioConfig {

    private static final Logger log = LoggerFactory.getLogger(TwilioConfig.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.verify-service-sid}")
    private String verifyServiceSid;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio SDK initialisé avec le compte SID : {}...", accountSid.substring(0, Math.min(8, accountSid.length())));
    }

    public String getVerifyServiceSid() {
        return verifyServiceSid;
    }
}
