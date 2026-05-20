package com.medical.patient.dto;

import com.medical.patient.entity.Sexe;

import java.time.LocalDate;

/**
 * Réponse d'authentification — token JWT + informations du patient.
 */
public class AuthResponse {

    private String token;
    private Long patientId;
    private String email;
    private String prenom;
    private String nom;
    private Sexe sexe;
    private LocalDate dateNaissance;
    private String telephone;
    private boolean emailVerifie;
    private boolean telephoneVerifie;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long patientId, String email, String prenom,
                         String nom, Sexe sexe, LocalDate dateNaissance, String telephone,
                         boolean emailVerifie, boolean telephoneVerifie) {
        this.token = token;
        this.patientId = patientId;
        this.email = email;
        this.prenom = prenom;
        this.nom = nom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.telephone = telephone;
        this.emailVerifie = emailVerifie;
        this.telephoneVerifie = telephoneVerifie;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public boolean isEmailVerifie() {
        return emailVerifie;
    }

    public void setEmailVerifie(boolean emailVerifie) {
        this.emailVerifie = emailVerifie;
    }

    public boolean isTelephoneVerifie() {
        return telephoneVerifie;
    }

    public void setTelephoneVerifie(boolean telephoneVerifie) {
        this.telephoneVerifie = telephoneVerifie;
    }
}
