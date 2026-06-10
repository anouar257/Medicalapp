package com.medical.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

  @Id
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Column(columnDefinition = "TEXT")
  private String antecedents;

  @Column(columnDefinition = "TEXT")
  private String symptomes;

  @Column(columnDefinition = "TEXT")
  private String consultation;

  @Column(columnDefinition = "TEXT")
  private String diagnostique;

  @Column(columnDefinition = "TEXT")
  private String consommable;

  @Column(columnDefinition = "TEXT")
  private String acte;

  @Column(columnDefinition = "TEXT")
  private String radiologie;

  public MedicalRecord() {
    // Constructeur vide requis par JPA/Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Appointment getAppointment() {
    return appointment;
  }

  public void setAppointment(Appointment appointment) {
    this.appointment = appointment;
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
