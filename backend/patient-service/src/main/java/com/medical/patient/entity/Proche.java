package com.medical.patient.entity;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entité Proche — formulaire conditionnel complet du cahier des charges.
 *
 * <p>Champs couverts :
 * <ul>
 *   <li>Identité : civilité, sexe, prénom, nom, nom de famille changé (conditionnel)</li>
 *   <li>Naissance : date, pays, ville</li>
 *   <li>Contacts : tél mobile, tél fixe, email</li>
 *   <li>Adresse : adresse, code postal, ville</li>
 *   <li>Médical : assurance, médecin traitant, provenance, profession, remarque</li>
 *   <li>Toggles : envoi SMS activé, envoi email activé</li>
 *   <li>Alertes : validation pièce identité, identité douteuse</li>
 * </ul>
 */
@Entity
@Table(name = "proches")
public class Proche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Patient responsable qui a ajouté ce proche. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // ── Identité ─────────────────────────────────────────────────────────────

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Civilite civilite;

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

    /** Indique si le nom de famille a été changé (toggle conditionnel). */
    @Column(name = "nom_famille_change", nullable = false)
    private boolean nomFamilleChange = false;

    /** Ancien nom de famille (affiché uniquement si nomFamilleChange = true). */
    @Column(name = "ancien_nom_famille", length = 100)
    private String ancienNomFamille;

    // ── Naissance ────────────────────────────────────────────────────────────

    @NotNull
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @NotBlank
    @Column(name = "pays_naissance", nullable = false, length = 100)
    private String paysNaissance;

    @NotBlank
    @Column(name = "ville_naissance", nullable = false, length = 100)
    private String villeNaissance;

    // ── Contacts ─────────────────────────────────────────────────────────────

    @NotBlank
    @Column(name = "telephone_mobile", nullable = false, length = 20)
    private String telephoneMobile;

    @Column(name = "telephone_fixe", length = 20)
    private String telephoneFixe;

    @NotBlank
    @Email
    @Column(nullable = false, length = 255)
    private String email;

    // ── Adresse ──────────────────────────────────────────────────────────────

    @NotBlank
    @Column(nullable = false, length = 500)
    private String adresse;

    @NotBlank
    @Column(name = "code_postal", nullable = false, length = 10)
    private String codePostal;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String ville;

    // ── Informations médicales / administratives ─────────────────────────────

    @Column(length = 255)
    private String assurance;

    @Column(length = 1000)
    private String remarque;

    @Column(length = 255)
    private String provenance;

    @Column(length = 255)
    private String profession;

    @Column(name = "medecin_traitant", length = 255)
    private String medecinTraitant;

    // ── Toggles SMS / Email ──────────────────────────────────────────────────

    @Column(name = "envoi_sms_active", nullable = false)
    private boolean envoiSmsActive = true;

    @Column(name = "envoi_email_active", nullable = false)
    private boolean envoiEmailActive = true;

    // ── Validation identité ──────────────────────────────────────────────────

    /** Pièce d'identité vérifiée et validée. */
    @Column(name = "piece_identite_validee", nullable = false)
    private boolean pieceIdentiteValidee = false;

    /** Signaler une identité douteuse ou fictive. */
    @Column(name = "identite_douteuse", nullable = false)
    private boolean identiteDouteuse = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Civilite getCivilite() {
        return civilite;
    }

    public void setCivilite(Civilite civilite) {
        this.civilite = civilite;
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

    public boolean isNomFamilleChange() {
        return nomFamilleChange;
    }

    public void setNomFamilleChange(boolean nomFamilleChange) {
        this.nomFamilleChange = nomFamilleChange;
    }

    public String getAncienNomFamille() {
        return ancienNomFamille;
    }

    public void setAncienNomFamille(String ancienNomFamille) {
        this.ancienNomFamille = ancienNomFamille;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getPaysNaissance() {
        return paysNaissance;
    }

    public void setPaysNaissance(String paysNaissance) {
        this.paysNaissance = paysNaissance;
    }

    public String getVilleNaissance() {
        return villeNaissance;
    }

    public void setVilleNaissance(String villeNaissance) {
        this.villeNaissance = villeNaissance;
    }

    public String getTelephoneMobile() {
        return telephoneMobile;
    }

    public void setTelephoneMobile(String telephoneMobile) {
        this.telephoneMobile = telephoneMobile;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public void setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getAssurance() {
        return assurance;
    }

    public void setAssurance(String assurance) {
        this.assurance = assurance;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getMedecinTraitant() {
        return medecinTraitant;
    }

    public void setMedecinTraitant(String medecinTraitant) {
        this.medecinTraitant = medecinTraitant;
    }

    public boolean isEnvoiSmsActive() {
        return envoiSmsActive;
    }

    public void setEnvoiSmsActive(boolean envoiSmsActive) {
        this.envoiSmsActive = envoiSmsActive;
    }

    public boolean isEnvoiEmailActive() {
        return envoiEmailActive;
    }

    public void setEnvoiEmailActive(boolean envoiEmailActive) {
        this.envoiEmailActive = envoiEmailActive;
    }

    public boolean isPieceIdentiteValidee() {
        return pieceIdentiteValidee;
    }

    public void setPieceIdentiteValidee(boolean pieceIdentiteValidee) {
        this.pieceIdentiteValidee = pieceIdentiteValidee;
    }

    public boolean isIdentiteDouteuse() {
        return identiteDouteuse;
    }

    public void setIdentiteDouteuse(boolean identiteDouteuse) {
        this.identiteDouteuse = identiteDouteuse;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }
}
