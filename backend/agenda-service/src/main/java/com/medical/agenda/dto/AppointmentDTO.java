package com.medical.agenda.dto;

import com.medical.agenda.entity.AppointmentStatus;
import java.time.Instant;

/**
 * Contrat JSON public d’un rendez-vous — aligné sur le frontend Angular.
 *
 * <p>Champs sérialisés (camelCase, défaut Jackson) :
 * {@code id}, {@code title}, {@code typeId}, {@code typeCode}, {@code typeLabel},
 * {@code typeColor}, {@code startTime}, {@code endTime} (ISO-8601), {@code durationMinutes},
 * {@code description}, {@code doctorId}, {@code color}, {@code status}.
 */
public class AppointmentDTO {

  private Long id;
  private String title;

  /** Identifiant du patient (stocké côté agenda-service). */
  private Long patientId;

  /** Copie affichage pour le cabinet (assistant) — pas d'appel patient-service. */
  private String patientPrenom;

  private String patientNom;

  /** Code motif (questionnaire RDV). */
  private String visitReasonCode;

  /** Référence vers la table {@code appointment_types} (obligatoire en POST/PUT). */
  private Long typeId;

  /** Recopiés en lecture pour éviter un round-trip côté frontend (sidebar, badges). */
  private String typeCode;
  private String typeLabel;
  private String typeColor;

  private Instant startTime;
  private Instant endTime;
  private Integer durationMinutes;
  private String description;
  private Long doctorId;
  private String color;

  /** Champs “fusionnés” pour simplifier l'affichage UI côté patient. */
  private String doctorName;
  private String doctorSpecialty;

  /** {@code PractitionerProfile.id} côté practitioner-service (messagerie, synchro). */
  private Long doctorExternalPractitionerId;

  private AppointmentStatus status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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

  public String getVisitReasonCode() {
    return visitReasonCode;
  }

  public void setVisitReasonCode(String visitReasonCode) {
    this.visitReasonCode = visitReasonCode;
  }

  public Long getTypeId() {
    return typeId;
  }

  public void setTypeId(Long typeId) {
    this.typeId = typeId;
  }

  public String getTypeCode() {
    return typeCode;
  }

  public void setTypeCode(String typeCode) {
    this.typeCode = typeCode;
  }

  public String getTypeLabel() {
    return typeLabel;
  }

  public void setTypeLabel(String typeLabel) {
    this.typeLabel = typeLabel;
  }

  public String getTypeColor() {
    return typeColor;
  }

  public void setTypeColor(String typeColor) {
    this.typeColor = typeColor;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getDoctorName() {
    return doctorName;
  }

  public void setDoctorName(String doctorName) {
    this.doctorName = doctorName;
  }

  public String getDoctorSpecialty() {
    return doctorSpecialty;
  }

  public void setDoctorSpecialty(String doctorSpecialty) {
    this.doctorSpecialty = doctorSpecialty;
  }

  public Long getDoctorExternalPractitionerId() {
    return doctorExternalPractitionerId;
  }

  public void setDoctorExternalPractitionerId(Long doctorExternalPractitionerId) {
    this.doctorExternalPractitionerId = doctorExternalPractitionerId;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }
}
