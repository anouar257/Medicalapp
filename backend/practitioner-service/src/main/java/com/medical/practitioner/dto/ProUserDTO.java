package com.medical.practitioner.dto;

import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;

public class ProUserDTO {

    private Long id;
    private String email;
    private String telephone;
    private String prenom;
    private String nom;
    private ProUserRole role;
    private boolean emailVerifie;
    private boolean telephoneVerifie;
    private boolean actif;
    private Long organizationId;
    private Long practitionerProfileId;

    public static ProUserDTO fromEntity(ProUser u) {
        ProUserDTO dto = new ProUserDTO();
        dto.id = u.getId();
        dto.email = u.getEmail();
        dto.telephone = u.getTelephone();
        dto.prenom = u.getPrenom();
        dto.nom = u.getNom();
        dto.role = u.getRole();
        dto.emailVerifie = u.isEmailVerifie();
        dto.telephoneVerifie = u.isTelephoneVerifie();
        dto.actif = u.isActif();
        if (u.getOrganization() != null) {
            dto.organizationId = u.getOrganization().getId();
        }
        if (u.getPractitionerProfile() != null) {
            dto.practitionerProfileId = u.getPractitionerProfile().getId();
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public boolean isEmailVerifie() { return emailVerifie; }
    public void setEmailVerifie(boolean emailVerifie) { this.emailVerifie = emailVerifie; }

    public boolean isTelephoneVerifie() { return telephoneVerifie; }
    public void setTelephoneVerifie(boolean telephoneVerifie) { this.telephoneVerifie = telephoneVerifie; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public Long getPractitionerProfileId() { return practitionerProfileId; }
    public void setPractitionerProfileId(Long practitionerProfileId) { this.practitionerProfileId = practitionerProfileId; }
}
