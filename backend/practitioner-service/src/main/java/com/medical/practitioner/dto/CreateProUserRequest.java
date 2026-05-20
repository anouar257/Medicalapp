package com.medical.practitioner.dto;

import com.medical.practitioner.entity.ProUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Création d'un compte utilisateur secondaire par l'admin du cabinet
 * (cf. cahier : « Créer un compte utilisateur — secrétaire, assistant, ... »).
 *
 * <p>Le mot de passe initial est généré et envoyé par email/SMS si non fourni.
 */
public class CreateProUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String telephone;

    @NotBlank
    private String prenom;

    @NotBlank
    private String nom;

    @NotNull
    private ProUserRole role;

    /** Mot de passe initial (optionnel — si null, un mot de passe temporaire est généré). */
    private String motDePasse;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public ProUserRole getRole() { return role; }
    public void setRole(ProUserRole role) { this.role = role; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
}
