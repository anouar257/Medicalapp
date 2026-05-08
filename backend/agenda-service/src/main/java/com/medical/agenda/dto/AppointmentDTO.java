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

  public AppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }
}
