package com.medical.practitioner.dto;

import com.medical.practitioner.entity.MedicalOrganization;

public class MedicalOrganizationDTO {

    private Long id;
    private String nom;
    private String siret;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private boolean actif;

    public static MedicalOrganizationDTO fromEntity(MedicalOrganization m) {
        MedicalOrganizationDTO dto = new MedicalOrganizationDTO();
        dto.id = m.getId();
        dto.nom = m.getNom();
        dto.siret = m.getSiret();
        dto.email = m.getEmail();
        dto.telephone = m.getTelephone();
        dto.adresse = m.getAdresse();
        dto.ville = m.getVille();
        dto.codePostal = m.getCodePostal();
        dto.pays = m.getPays();
        dto.actif = m.isActif();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getSiret() { return siret; }
    public void setSiret(String siret) { this.siret = siret; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
