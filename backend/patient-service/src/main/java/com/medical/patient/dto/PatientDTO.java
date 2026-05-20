package com.medical.patient.dto;

import java.time.Instant;

public class PatientDTO {
    private Long id;
    private String prenom;
    private String nom;
    private String email;
    private String telephone;
    private boolean actif;
    private Instant dateInscription;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Instant getDateInscription() { return dateInscription; }
    public void setDateInscription(Instant dateInscription) { this.dateInscription = dateInscription; }
}
