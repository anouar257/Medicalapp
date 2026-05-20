package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Diplôme, certification ou conférence d'un praticien.
 *
 * <p>Cf. cahier des charges : « Vérifier ses diplômes + certifications + conférences + youtube + ... ».
 * Une catégorie ({@link DiplomaType}) sépare les usages.
 */
@Entity
@Table(name = "diplomas")
public class Diploma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerProfile practitioner;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(length = 200)
    private String etablissement;

    @Column(name = "annee_obtention")
    private Integer anneeObtention;

    @Column(name = "date_obtention")
    private LocalDate dateObtention;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiplomaType type = DiplomaType.DIPLOME;

    @Column(name = "document_url", length = 512)
    private String documentUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PractitionerProfile getPractitioner() { return practitioner; }
    public void setPractitioner(PractitionerProfile practitioner) { this.practitioner = practitioner; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public Integer getAnneeObtention() { return anneeObtention; }
    public void setAnneeObtention(Integer anneeObtention) { this.anneeObtention = anneeObtention; }

    public LocalDate getDateObtention() { return dateObtention; }
    public void setDateObtention(LocalDate dateObtention) { this.dateObtention = dateObtention; }

    public DiplomaType getType() { return type; }
    public void setType(DiplomaType type) { this.type = type; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public enum DiplomaType {
        DIPLOME,
        CERTIFICATION,
        CONFERENCE,
        FORMATION
    }
}
