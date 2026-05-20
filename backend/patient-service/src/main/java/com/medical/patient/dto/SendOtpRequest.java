package com.medical.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour demander l'envoi d'un code OTP.
 */
public class SendOtpRequest {

    /** Email ou téléphone du destinataire. */
    @NotBlank(message = "Le destinataire est obligatoire")
    private String to;

    /** Canal : "sms" ou "email". */
    @NotBlank(message = "Le canal est obligatoire (sms ou email)")
    private String channel;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
