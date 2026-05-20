package com.medical.practitioner.dto;

import com.medical.practitioner.entity.ProUserRole;

/**
 * Réponse d'authentification pro — token JWT + données utilisateur (avec rôle exact).
 */
public class AuthProResponse {

    private String token;
    private Long userId;
    private String email;
    private String telephone;
    private String prenom;
    private String nom;
    private ProUserRole role;
    private Long organizationId;
    private String organizationNom;
    private boolean emailVerifie;
    private boolean telephoneVerifie;

    /** Identifiant du profil praticien (null si l'utilisateur n'est pas un PRATICIEN). */
    private Long practitionerProfileId;

    public AuthProResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getOrganizationNom() { return organizationNom; }
    public void setOrganizationNom(String organizationNom) { this.organizationNom = organizationNom; }

    public boolean isEmailVerifie() { return emailVerifie; }
    public void setEmailVerifie(boolean emailVerifie) { this.emailVerifie = emailVerifie; }

    public boolean isTelephoneVerifie() { return telephoneVerifie; }
    public void setTelephoneVerifie(boolean telephoneVerifie) { this.telephoneVerifie = telephoneVerifie; }

    public Long getPractitionerProfileId() { return practitionerProfileId; }
    public void setPractitionerProfileId(Long practitionerProfileId) { this.practitionerProfileId = practitionerProfileId; }
}
