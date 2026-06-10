package com.medical.agenda.dto;

public class MedicalRecordDTO {

  private Long appointmentId;
  private String antecedents;
  private String symptomes;
  private String consultation;
  private String diagnostique;
  private String consommable;
  private String acte;
  private String radiologie;

  public MedicalRecordDTO() {
    // Constructeur vide requis par Jackson pour la désérialisation
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Long appointmentId) {
    this.appointmentId = appointmentId;
  }

  public String getAntecedents() {
    return antecedents;
  }

  public void setAntecedents(String antecedents) {
    this.antecedents = antecedents;
  }

  public String getSymptomes() {
    return symptomes;
  }

  public void setSymptomes(String symptomes) {
    this.symptomes = symptomes;
  }

  public String getConsultation() {
    return consultation;
  }

  public void setConsultation(String consultation) {
    this.consultation = consultation;
  }

  public String getDiagnostique() {
    return diagnostique;
  }

  public void setDiagnostique(String diagnostique) {
    this.diagnostique = diagnostique;
  }

  public String getConsommable() {
    return consommable;
  }

  public void setConsommable(String consommable) {
    this.consommable = consommable;
  }

  public String getActe() {
    return acte;
  }

  public void setActe(String acte) {
    this.acte = acte;
  }

  public String getRadiologie() {
    return radiologie;
  }

  public void setRadiologie(String radiologie) {
    this.radiologie = radiologie;
  }
}
