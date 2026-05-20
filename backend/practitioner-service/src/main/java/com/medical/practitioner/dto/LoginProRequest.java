package com.medical.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de connexion professionnelle.
 *
 * <p>Aucun choix de rôle ici : le rôle est lu en base puis renvoyé dans le token JWT.
 */
public class LoginProRequest {

    @NotBlank(message = "L'email professionnel est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
}
