package com.medical.practitioner.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Demande d'inscription complète d'un cabinet — crée :
 * <ul>
 *   <li>un {@link com.medical.practitioner.entity.MedicalOrganization} (le cabinet) ;</li>
 *   <li>un {@link com.medical.practitioner.entity.ProUser} {@code PRATICIEN} titulaire
 *   (créateur du cabinet, avec profil praticien) ;</li>
 *   <li>déclenche les OTP email + SMS pour la vérification.</li>
 * </ul>
 */
public class RegisterCabinetRequest {

    // ── Cabinet (organisme médical) ────────────────────────────────────────

    @NotBlank(message = "Le nom du cabinet est obligatoire")
    private String nomCabinet;

    private String siret;

    private String adresseCabinet;

    private String villeCabinet;

    private String codePostalCabinet;

    private String telephoneCabinet;

    // ── Compte praticien titulaire ───────────────────────────────────────────

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "L'email professionnel est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;

    @NotNull(message = "Vous devez accepter les conditions d'utilisation")
    private Boolean cguAcceptees;

    @AssertTrue(message = "Vous devez accepter les conditions d'utilisation")
    public boolean isCguOk() {
        return Boolean.TRUE.equals(cguAcceptees);
    }

    public String getNomCabinet() { return nomCabinet; }
    public void setNomCabinet(String nomCabinet) { this.nomCabinet = nomCabinet; }

    public String getSiret() { return siret; }
    public void setSiret(String siret) { this.siret = siret; }

    public String getAdresseCabinet() { return adresseCabinet; }
    public void setAdresseCabinet(String adresseCabinet) { this.adresseCabinet = adresseCabinet; }

    public String getVilleCabinet() { return villeCabinet; }
    public void setVilleCabinet(String villeCabinet) { this.villeCabinet = villeCabinet; }

    public String getCodePostalCabinet() { return codePostalCabinet; }
    public void setCodePostalCabinet(String codePostalCabinet) { this.codePostalCabinet = codePostalCabinet; }

    public String getTelephoneCabinet() { return telephoneCabinet; }
    public void setTelephoneCabinet(String telephoneCabinet) { this.telephoneCabinet = telephoneCabinet; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public Boolean getCguAcceptees() { return cguAcceptees; }
    public void setCguAcceptees(Boolean cguAcceptees) { this.cguAcceptees = cguAcceptees; }
}
