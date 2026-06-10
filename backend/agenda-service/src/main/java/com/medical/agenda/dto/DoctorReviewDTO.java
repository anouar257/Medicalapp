package com.medical.agenda.dto;

import java.time.Instant;

public class DoctorReviewDTO {

  private Long id;
  private Long appointmentId;
  private Long externalPractitionerId;
  private String patientDisplayName;
  private Integer rating;
  private Integer punctualityRating;
  private String comment;
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Long appointmentId) {
    this.appointmentId = appointmentId;
  }

  public Long getExternalPractitionerId() {
    return externalPractitionerId;
  }

  public void setExternalPractitionerId(Long externalPractitionerId) {
    this.externalPractitionerId = externalPractitionerId;
  }

  public String getPatientDisplayName() {
    return patientDisplayName;
  }

  public void setPatientDisplayName(String patientDisplayName) {
    this.patientDisplayName = patientDisplayName;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public Integer getPunctualityRating() {
    return punctualityRating;
  }

  public void setPunctualityRating(Integer punctualityRating) {
    this.punctualityRating = punctualityRating;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
