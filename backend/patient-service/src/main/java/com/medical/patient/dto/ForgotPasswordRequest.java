package com.medical.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la demande de réinitialisation du mot de passe.
 * Le patient fournit son email ou son téléphone.
 */
public class ForgotPasswordRequest {

    /** Email ou téléphone du patient. */
    @NotBlank(message = "L'identifiant (email ou téléphone) est obligatoire")
    private String identifiant;

    /** Canal de réinitialisation : "email" ou "sms". */
    @NotBlank(message = "Le canal est obligatoire (email ou sms)")
    private String channel;

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
