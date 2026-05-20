package com.medical.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String nouveauMotDePasse;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNouveauMotDePasse() { return nouveauMotDePasse; }
    public void setNouveauMotDePasse(String nouveauMotDePasse) { this.nouveauMotDePasse = nouveauMotDePasse; }
}
