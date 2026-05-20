package com.medical.agenda.dto;

import java.time.Instant;

/** Demande de rendez-vous côté patient (statut {@code PENDING} à la création). */
public class PatientBookingRequestDTO {

  private Long doctorId;
  private Long patientId;

  /** Affichage cabinet (assistant) — optionnel si absent, dérivé côté client ou vide. */
  private String patientPrenom;

  private String patientNom;

  private String typeCode;
  private Instant startTime;
  private Integer durationMinutes;
  private String title;
  private String color;
  /** CABINET | CLINIC | REMOTE */
  private String locationMode;

  private String referredBy;
  private String priorCareCode;
  private String visitReasonCode;
  private String beneficiarySummary;
  private String mapQuery;

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public String getPatientPrenom() {
    return patientPrenom;
  }

  public void setPatientPrenom(String patientPrenom) {
    this.patientPrenom = patientPrenom;
  }

  public String getPatientNom() {
    return patientNom;
  }

  public void setPatientNom(String patientNom) {
    this.patientNom = patientNom;
  }

  public String getTypeCode() {
    return typeCode;
  }

  public void setTypeCode(String typeCode) {
    this.typeCode = typeCode;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getLocationMode() {
    return locationMode;
  }

  public void setLocationMode(String locationMode) {
    this.locationMode = locationMode;
  }

  public String getReferredBy() {
    return referredBy;
  }

  public void setReferredBy(String referredBy) {
    this.referredBy = referredBy;
  }

  public String getPriorCareCode() {
    return priorCareCode;
  }

  public void setPriorCareCode(String priorCareCode) {
    this.priorCareCode = priorCareCode;
  }

  public String getVisitReasonCode() {
    return visitReasonCode;
  }

  public void setVisitReasonCode(String visitReasonCode) {
    this.visitReasonCode = visitReasonCode;
  }

  public String getBeneficiarySummary() {
    return beneficiarySummary;
  }

  public void setBeneficiarySummary(String beneficiarySummary) {
    this.beneficiarySummary = beneficiarySummary;
  }

  public String getMapQuery() {
    return mapQuery;
  }

  public void setMapQuery(String mapQuery) {
    this.mapQuery = mapQuery;
  }
}
