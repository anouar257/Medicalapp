package com.medical.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Patient — fidèle au cahier des charges (PARTIE DU PATIENT).
 *
 * <p>Champs couverts :
 * <ul>
 *   <li>Inscription : sexe, prénom, nom, date de naissance, mot de passe, CGU</li>
 *   <li>Vérification : email (vérifié), téléphone (vérifié par OTP Twilio)</li>
 *   <li>Mot de passe oublié : resetToken + resetTokenExpiry</li>
 * </ul>
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sexe sexe;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String prenom;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nom;

    @NotNull
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    /** Hash BCrypt du mot de passe. */
    @NotBlank
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    /** Email confirmé par code OTP Twilio Verify. */
    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie = false;

    /** Téléphone confirmé par OTP SMS Twilio Verify. */
    @Column(name = "telephone_verifie", nullable = false)
    private boolean telephoneVerifie = false;

    /** Conditions Générales d'Utilisation acceptées à l'inscription. */
    @Column(name = "cgu_acceptees", nullable = false)
    private boolean cguAcceptees = false;

    @Column(name = "date_inscription", nullable = false, updatable = false)
    private Instant dateInscription = Instant.now();

    /** Jeton unique envoyé par email/SMS pour réinitialiser le mot de passe. */
    @Column(name = "reset_token", length = 255)
    private String resetToken;

    /** Expiration du jeton de réinitialisation. */
    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    /** Compte actif (peut être désactivé par un admin). */
    @Column(nullable = false)
    private boolean actif = true;

    /** Liste des proches rattachés à ce patient. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "patient", orphanRemoval = true)
    private List<Proche> proches = new ArrayList<>();

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
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

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
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

    public boolean isCguAcceptees() {
        return cguAcceptees;
    }

    public void setCguAcceptees(boolean cguAcceptees) {
        this.cguAcceptees = cguAcceptees;
    }

    public Instant getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Instant dateInscription) {
        this.dateInscription = dateInscription;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Instant getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(Instant resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public List<Proche> getProches() {
        return proches;
    }

    public void setProches(List<Proche> proches) {
        this.proches = proches;
    }
}
