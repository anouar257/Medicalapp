package com.medical.practitioner.dto;

import com.medical.practitioner.entity.Civilite;
import com.medical.practitioner.entity.Sexe;
import com.medical.practitioner.entity.StatutPraticien;
import com.medical.practitioner.entity.Titre;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Inscription d'un praticien (ou d'un remplaçant) en self-service.
 *
 * <p>Cf. cahier des charges (section INSCRIPTION DU PRATICIEN OU UN REMPLAÇANT) :
 * <ul>
 *   <li>civilité, titre, sexe, prénom, nom, date de naissance ;</li>
 *   <li>email, téléphone (préalablement vérifiés) ;</li>
 *   <li>mot de passe + acceptation CGU ;</li>
 *   <li>statut (titulaire, remplaçant, etc.) ;</li>
 *   <li>liste d'identifiants de spécialités choisies.</li>
 * </ul>
 *
 * <p>Le cabinet de rattachement est facultatif : un praticien indépendant peut s'inscrire
 * sans rejoindre un cabinet existant ; un cabinet est alors automatiquement créé pour lui.
 */
public class RegisterPractitionerRequest {

    private Civilite civilite;

    @NotNull(message = "Le titre est obligatoire (AUCUN par défaut)")
    private Titre titre = Titre.AUCUN;

    @NotNull(message = "Le sexe est obligatoire")
    private Sexe sexe;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    @NotBlank(message = "L'email est obligatoire")
    @Email
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;

    @NotNull(message = "Vous devez accepter les conditions d'utilisation")
    private Boolean cguAcceptees;

    private StatutPraticien statut = StatutPraticien.TITULAIRE;

    /** Identifiants de spécialités sélectionnées (au moins une). */
    private List<Long> specialiteIds;

    /** Identifiant du cabinet à rejoindre (optionnel — sinon un cabinet personnel est créé). */
    private Long organizationId;

    /** Nom du cabinet personnel à créer si {@code organizationId} est null (optionnel). */
    private String nomCabinetPersonnel;

    @AssertTrue(message = "Vous devez accepter les conditions d'utilisation")
    public boolean isCguOk() {
        return Boolean.TRUE.equals(cguAcceptees);
    }

    public Civilite getCivilite() { return civilite; }
    public void setCivilite(Civilite civilite) { this.civilite = civilite; }

    public Titre getTitre() { return titre; }
    public void setTitre(Titre titre) { this.titre = titre; }

    public Sexe getSexe() { return sexe; }
    public void setSexe(Sexe sexe) { this.sexe = sexe; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public Boolean getCguAcceptees() { return cguAcceptees; }
    public void setCguAcceptees(Boolean cguAcceptees) { this.cguAcceptees = cguAcceptees; }

    public StatutPraticien getStatut() { return statut; }
    public void setStatut(StatutPraticien statut) { this.statut = statut; }

    public List<Long> getSpecialiteIds() { return specialiteIds; }
    public void setSpecialiteIds(List<Long> specialiteIds) { this.specialiteIds = specialiteIds; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getNomCabinetPersonnel() { return nomCabinetPersonnel; }
    public void setNomCabinetPersonnel(String nomCabinetPersonnel) { this.nomCabinetPersonnel = nomCabinetPersonnel; }
}
