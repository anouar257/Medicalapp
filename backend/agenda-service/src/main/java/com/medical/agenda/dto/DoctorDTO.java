package com.medical.agenda.dto;

public class DoctorDTO {

  private Long id;
  private String name;
  private String colorCode;
  private String photoUrl;

  /** Nombre de rendez-vous en base liés à ce médecin (liste / admin). */
  private long appointmentCount;

  /** Spécialité médicale affichée (recherche, fiches). */
  private String specialty;

  /** Code spécialité référentiel (synchro practitioner). */
  private String specialtyCode;

  /** Lien vers {@code PractitionerProfile.id} si le médecin agenda provient de la synchro pro. */
  private Long externalPractitionerId;

  /** Cabinet (practitioner-service) — synchro pour filtrage demandes RDV. */
  private Long organizationId;

  public Long getExternalPractitionerId() {
    return externalPractitionerId;
  }

  public void setExternalPractitionerId(Long externalPractitionerId) {
    this.externalPractitionerId = externalPractitionerId;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
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

  public long getAppointmentCount() {
    return appointmentCount;
  }

  public void setAppointmentCount(long appointmentCount) {
    this.appointmentCount = appointmentCount;
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
}
