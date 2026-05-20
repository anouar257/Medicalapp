package com.medical.practitioner.dto;

import com.medical.practitioner.entity.Civilite;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.Sexe;
import com.medical.practitioner.entity.StatutPraticien;
import com.medical.practitioner.entity.Titre;
import com.medical.practitioner.entity.VerificationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO de lecture/écriture du profil praticien.
 */
public class PractitionerProfileDTO {

    private Long id;
    private Long proUserId;
    private String email;
    private String telephone;
    private Civilite civilite;
    private Titre titre;
    private Sexe sexe;
    private String prenom;
    private String nom;
    private LocalDate dateNaissance;
    private StatutPraticien statut;
    private String empreinte;
    private String lienYoutube;
    private String siteWeb;
    private String biographie;
    private String photoUrl;
    private String colorCode;
    private VerificationStatus verifIdentiteStatus;
    private VerificationStatus verifDroitExercerStatus;
    private boolean disponible;
    private Set<SpecialtyDTO> specialites;
    private List<DiplomaDTO> diplomes;
    private Long organizationId;
    private String organizationNom;

    public static PractitionerProfileDTO fromEntity(PractitionerProfile p) {
        PractitionerProfileDTO dto = new PractitionerProfileDTO();
        dto.id = p.getId();
        if (p.getProUser() != null) {
            dto.proUserId = p.getProUser().getId();
            dto.email = p.getProUser().getEmail();
            dto.telephone = p.getProUser().getTelephone();
            dto.prenom = p.getProUser().getPrenom();
            dto.nom = p.getProUser().getNom();
            if (p.getProUser().getOrganization() != null) {
                dto.organizationId = p.getProUser().getOrganization().getId();
                dto.organizationNom = p.getProUser().getOrganization().getNom();
            }
        }
        dto.civilite = p.getCivilite();
        dto.titre = p.getTitre();
        dto.sexe = p.getSexe();
        dto.dateNaissance = p.getDateNaissance();
        dto.statut = p.getStatut();
        dto.empreinte = p.getEmpreinte();
        dto.lienYoutube = p.getLienYoutube();
        dto.siteWeb = p.getSiteWeb();
        dto.biographie = p.getBiographie();
        dto.photoUrl = p.getPhotoUrl();
        dto.colorCode = p.getColorCode();
        dto.verifIdentiteStatus = p.getVerifIdentiteStatus();
        dto.verifDroitExercerStatus = p.getVerifDroitExercerStatus();
        dto.disponible = p.isDisponible();
        if (p.getSpecialites() != null) {
            dto.specialites = p.getSpecialites().stream()
                    .map(s -> new SpecialtyDTO(s.getId(), s.getCode(), s.getLibelle()))
                    .collect(Collectors.toSet());
        }
        if (p.getDiplomes() != null) {
            dto.diplomes = p.getDiplomes().stream().map(DiplomaDTO::fromEntity).toList();
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProUserId() { return proUserId; }
    public void setProUserId(Long proUserId) { this.proUserId = proUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

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

    public StatutPraticien getStatut() { return statut; }
    public void setStatut(StatutPraticien statut) { this.statut = statut; }

    public String getEmpreinte() { return empreinte; }
    public void setEmpreinte(String empreinte) { this.empreinte = empreinte; }

    public String getLienYoutube() { return lienYoutube; }
    public void setLienYoutube(String lienYoutube) { this.lienYoutube = lienYoutube; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getBiographie() { return biographie; }
    public void setBiographie(String biographie) { this.biographie = biographie; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public VerificationStatus getVerifIdentiteStatus() { return verifIdentiteStatus; }
    public void setVerifIdentiteStatus(VerificationStatus verifIdentiteStatus) { this.verifIdentiteStatus = verifIdentiteStatus; }

    public VerificationStatus getVerifDroitExercerStatus() { return verifDroitExercerStatus; }
    public void setVerifDroitExercerStatus(VerificationStatus verifDroitExercerStatus) { this.verifDroitExercerStatus = verifDroitExercerStatus; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public Set<SpecialtyDTO> getSpecialites() { return specialites; }
    public void setSpecialites(Set<SpecialtyDTO> specialites) { this.specialites = specialites; }

    public List<DiplomaDTO> getDiplomes() { return diplomes; }
    public void setDiplomes(List<DiplomaDTO> diplomes) { this.diplomes = diplomes; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getOrganizationNom() { return organizationNom; }
    public void setOrganizationNom(String organizationNom) { this.organizationNom = organizationNom; }
}
