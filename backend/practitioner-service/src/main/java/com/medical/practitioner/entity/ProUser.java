package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Compte utilisateur professionnel — rôle parmi {@link ProUserRole}
 * (administration plateforme, praticien, assistant).
 */
@Entity
@Table(name = "pro_users")
public class ProUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private MedicalOrganization organization;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String telephone;

    /** Hash BCrypt du mot de passe. */
    @NotBlank
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProUserRole role = ProUserRole.PRATICIEN;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 100)
    private String nom;

    /** Email confirmé par OTP Twilio Verify. */
    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie = false;

    /** Téléphone confirmé par OTP SMS Twilio Verify. */
    @Column(name = "telephone_verifie", nullable = false)
    private boolean telephoneVerifie = false;

    /** Conditions générales acceptées à l'inscription (obligatoire). */
    @Column(name = "cgu_acceptees", nullable = false)
    private boolean cguAcceptees = false;

    /** Compte actif (peut être désactivé par l'admin du cabinet). */
    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_inscription", nullable = false, updatable = false)
    private Instant dateInscription = Instant.now();

    /** Token unique pour la réinitialisation par email. */
    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Instant resetTokenExpiry;

    /** Profil détaillé — uniquement si le rôle est PRATICIEN. */
    @OneToOne(mappedBy = "proUser", fetch = FetchType.LAZY, orphanRemoval = true)
    private PractitionerProfile practitionerProfile;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MedicalOrganization getOrganization() { return organization; }
    public void setOrganization(MedicalOrganization organization) { this.organization = organization; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public ProUserRole getRole() { return role; }
    public void setRole(ProUserRole role) { this.role = role; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public boolean isEmailVerifie() { return emailVerifie; }
    public void setEmailVerifie(boolean emailVerifie) { this.emailVerifie = emailVerifie; }

    public boolean isTelephoneVerifie() { return telephoneVerifie; }
    public void setTelephoneVerifie(boolean telephoneVerifie) { this.telephoneVerifie = telephoneVerifie; }

    public boolean isCguAcceptees() { return cguAcceptees; }
    public void setCguAcceptees(boolean cguAcceptees) { this.cguAcceptees = cguAcceptees; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Instant getDateInscription() { return dateInscription; }
    public void setDateInscription(Instant dateInscription) { this.dateInscription = dateInscription; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public Instant getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(Instant resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public PractitionerProfile getPractitionerProfile() { return practitionerProfile; }
    public void setPractitionerProfile(PractitionerProfile practitionerProfile) { this.practitionerProfile = practitionerProfile; }
}
