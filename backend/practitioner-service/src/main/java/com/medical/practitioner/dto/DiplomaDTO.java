package com.medical.practitioner.dto;

import com.medical.practitioner.entity.Diploma;

import java.time.LocalDate;

public class DiplomaDTO {

    private Long id;
    private String intitule;
    private String etablissement;
    private Integer anneeObtention;
    private LocalDate dateObtention;
    private Diploma.DiplomaType type;
    private String documentUrl;

    public static DiplomaDTO fromEntity(Diploma d) {
        DiplomaDTO dto = new DiplomaDTO();
        dto.id = d.getId();
        dto.intitule = d.getIntitule();
        dto.etablissement = d.getEtablissement();
        dto.anneeObtention = d.getAnneeObtention();
        dto.dateObtention = d.getDateObtention();
        dto.type = d.getType();
        dto.documentUrl = d.getDocumentUrl();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public Integer getAnneeObtention() { return anneeObtention; }
    public void setAnneeObtention(Integer anneeObtention) { this.anneeObtention = anneeObtention; }

    public LocalDate getDateObtention() { return dateObtention; }
    public void setDateObtention(LocalDate dateObtention) { this.dateObtention = dateObtention; }

    public Diploma.DiplomaType getType() { return type; }
    public void setType(Diploma.DiplomaType type) { this.type = type; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
}
