package com.medical.practitioner.dto;

/**
 * Résultat de recherche “public” pour le typeahead de la Landing Page.
 *
 * <p>Le frontend affiche :
 * <ul>
 *   <li>Nom du praticien</li>
 *   <li>Spécialité</li>
 *   <li>Ville</li>
 * </ul>
 */
public class PractitionerSearchResultDTO {

  private Long practitionerId;
  private String nom;
  private String specialty;
  private String ville;

  /** Code référentiel de la spécialité principale (questionnaires / durées). */
  private String primarySpecialtyCode;

  public Long getPractitionerId() {
    return practitionerId;
  }

  public void setPractitionerId(Long practitionerId) {
    this.practitionerId = practitionerId;
  }

  public String getNom() {
    return nom;
  }

  public void setNom(String nom) {
    this.nom = nom;
  }

  public String getSpecialty() {
    return specialty;
  }

  public void setSpecialty(String specialty) {
    this.specialty = specialty;
  }

  public String getVille() {
    return ville;
  }

  public void setVille(String ville) {
    this.ville = ville;
  }

  public String getPrimarySpecialtyCode() {
    return primarySpecialtyCode;
  }

  public void setPrimarySpecialtyCode(String primarySpecialtyCode) {
    this.primarySpecialtyCode = primarySpecialtyCode;
  }
}

