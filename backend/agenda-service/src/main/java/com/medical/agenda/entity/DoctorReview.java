package com.medical.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "doctor_reviews",
    uniqueConstraints = @UniqueConstraint(name = "uk_review_appointment", columnNames = "appointment_id"))
public class DoctorReview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appointment_id", nullable = false)
  private Appointment appointment;

  @Column(name = "patient_id", nullable = false)
  private Long patientId;

  @Column(name = "patient_display_name", nullable = false, length = 160)
  private String patientDisplayName;

  @Column(name = "doctor_id", nullable = false)
  private Long doctorId;

  @Column(name = "external_practitioner_id", nullable = false)
  private Long externalPractitionerId;

  @Column(nullable = false)
  private Integer rating;

  @Column(name = "punctuality_rating", nullable = false)
  private Integer punctualityRating;

  @Column(length = 1200)
  private String comment;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

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

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public String getPatientDisplayName() {
    return patientDisplayName;
  }

  public void setPatientDisplayName(String patientDisplayName) {
    this.patientDisplayName = patientDisplayName;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getExternalPractitionerId() {
    return externalPractitionerId;
  }

  public void setExternalPractitionerId(Long externalPractitionerId) {
    this.externalPractitionerId = externalPractitionerId;
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
