package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Catalogue de spécialités médicales.
 *
 * <p>Cf. cahier des charges : « Choisir une spécialité ou plus ».
 * Plusieurs praticiens peuvent partager la même spécialité (ManyToMany).
 */
@Entity
@Table(name = "specialties")
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(length = 500)
    private String description;

    public Specialty() {}

    public Specialty(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
