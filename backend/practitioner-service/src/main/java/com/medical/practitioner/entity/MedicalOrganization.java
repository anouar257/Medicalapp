package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Organisme médical (Cabinet) — racine de tous les comptes pro.
 *
 * <p>Cf. cahier des charges, section CABINET : « Créer un compte de l'organisme médical ».
 */
@Entity
@Table(name = "medical_organizations")
public class MedicalOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String nom;

    @Column(length = 50)
    private String siret;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(length = 500)
    private String adresse;

    @Column(name = "ville", length = 120)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(name = "pays", length = 100)
    private String pays = "France";

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();

    @Column(nullable = false)
    private boolean actif = true;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "organization", orphanRemoval = false)
    private List<ProUser> proUsers = new ArrayList<>();

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

    public Instant getDateCreation() { return dateCreation; }
    public void setDateCreation(Instant dateCreation) { this.dateCreation = dateCreation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public List<ProUser> getProUsers() { return proUsers; }
    public void setProUsers(List<ProUser> proUsers) { this.proUsers = proUsers; }
}
