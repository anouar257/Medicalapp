package com.medical.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de connexion patient (email ou téléphone + mot de passe).
 */
public class LoginRequest {

    @NotBlank(message = "L'identifiant (email ou téléphone) est obligatoire")
    private String identifiant;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
