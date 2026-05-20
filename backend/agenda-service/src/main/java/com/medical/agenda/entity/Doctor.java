package com.medical.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctors")
public class Doctor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "color_code", nullable = false, length = 16)
  private String colorCode;

  /**
   * URL de la photo affichée à côté du nom du médecin.
   *
   * <p>Volontairement {@code nullable=true} au niveau JPA pour autoriser une migration douce
   * (Hibernate {@code update} ne pourra pas ajouter une colonne NOT NULL à des lignes existantes).
   * La couche service applique malgré tout la règle <em>obligatoire</em> en remplissant un avatar
   * généré (UI Avatars) lorsque le client n’en fournit pas.
   */
  @Column(name = "photo_url", length = 512)
  private String photoUrl;

  /** Spécialité affichée (ex. Médecine générale, Cardiologie). */
  @Column(name = "specialty", length = 160)
  private String specialty;

  /** Code spécialité référentiel (ex. CHIRURGIE_DENTAIRE) — synchro practitioner. */
  @Column(name = "specialty_code", length = 64)
  private String specialtyCode;

  /** Cabinet (practitioner-service) pour isoler les demandes RDV web par organisation. */
  @Column(name = "organization_id")
  private Long organizationId;

  /**
   * Identifiant du {@code PractitionerProfile} dans le practitioner-service.
   *
   * <p>Quand le practitioner-service synchronise un praticien (création ou mise à jour),
   * il envoie cet ID externe. L'agenda-service crée ou met à jour le {@link Doctor}
   * correspondant. Permet de garder l'agenda <em>léger</em> (cache d'affichage) sans
   * jointures inter-services à chaque requête.
   *
   * <p>Reste {@code null} pour les médecins créés directement dans l'agenda (legacy ou admin).
   */
  @Column(name = "external_practitioner_id", unique = true)
  private Long externalPractitionerId;

  public Long getExternalPractitionerId() {
    return externalPractitionerId;
  }

  public void setExternalPractitionerId(Long externalPractitionerId) {
    this.externalPractitionerId = externalPractitionerId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getColorCode() {
    return colorCode;
  }

  public void setColorCode(String colorCode) {
    this.colorCode = colorCode;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }

  public String getSpecialty() {
    return specialty;
  }

  public void setSpecialty(String specialty) {
    this.specialty = specialty;
  }

  public String getSpecialtyCode() {
    return specialtyCode;
  }

  public void setSpecialtyCode(String specialtyCode) {
    this.specialtyCode = specialtyCode;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }
}
