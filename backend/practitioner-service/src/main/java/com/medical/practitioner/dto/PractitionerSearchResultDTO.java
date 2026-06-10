package com.medical.practitioner.dto;

import java.math.BigDecimal;

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

  /** URL de la photo de profil du praticien. */
  private String photoUrl;

  /** Adresse du cabinet (organisation). */
  private String adresse;

  private BigDecimal consultationFee;

  private Double globalRating;
  private Integer reviewCount;

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

  public String getPhotoUrl() {
    return photoUrl;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }

  public String getAdresse() {
    return adresse;
  }

  public void setAdresse(String adresse) {
    this.adresse = adresse;
  }

  public BigDecimal getConsultationFee() {
    return consultationFee;
  }

  public void setConsultationFee(BigDecimal consultationFee) {
    this.consultationFee = consultationFee;
  }

  public Double getGlobalRating() {
    return globalRating;
  }

  public void setGlobalRating(Double globalRating) {
    this.globalRating = globalRating;
  }

  public Integer getReviewCount() {
    return reviewCount;
  }

  public void setReviewCount(Integer reviewCount) {
    this.reviewCount = reviewCount;
  }

  private boolean hasMultipleLocations;

  public boolean isHasMultipleLocations() {
    return hasMultipleLocations;
  }

  public void setHasMultipleLocations(boolean hasMultipleLocations) {
    this.hasMultipleLocations = hasMultipleLocations;
  }
}
