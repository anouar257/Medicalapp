package com.medical.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank
    private String identifiant;

    /** "email" ou "sms". */
    @NotBlank
    private String channel;

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
