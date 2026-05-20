package com.medical.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de vérification OTP (email ou téléphone).
 */
public class VerifyOtpRequest {

    /** Email ou téléphone utilisé pour l'envoi du code. */
    @NotBlank(message = "Le destinataire (email ou téléphone) est obligatoire")
    private String to;

    @NotBlank(message = "Le code OTP est obligatoire")
    private String code;

    /** Canal utilisé : "sms" ou "email". */
    @NotBlank(message = "Le canal est obligatoire (sms ou email)")
    private String channel;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
