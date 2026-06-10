package com.medical.agenda.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  /**
   * Type de visite (référence dynamique). Volontairement {@code nullable=true} au niveau JPA pour
   * permettre la migration douce d’anciennes lignes (la couche service exige un type valide à la
   * création / modification).
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_type_id")
  private AppointmentType appointmentType;


  @Column(name = "start_time", nullable = false)
  private Instant startTime;

  @Column(name = "end_time", nullable = false)
  private Instant endTime;

  /** Durée en minutes (aligné frontend `durationMinutes`). */
  @Column(name = "duration_minutes", nullable = false)
  private Integer durationMinutes;

  @Column(length = 4000)
  private String description;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "doctor_id", nullable = false)
  private Doctor doctor;

  @Column(nullable = false, length = 16)
  private String color;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 32)
  private AppointmentStatus status;

  /**
   * Identifiant du patient (stockage simple côté agenda-service).
   * <p>
   * Permet au frontend patient d'afficher ses rendez-vous via
   * {@code GET /api/appointments/patient/{patientId}}.
   */
  @Column(name = "patient_id")
  private Long patientId;

  @Column(name = "patient_prenom", length = 120)
  private String patientPrenom;

  @Column(name = "patient_nom", length = 120)
  private String patientNom;

  @Column(name = "visit_reason_code", length = 64)
  private String visitReasonCode;

  @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private MedicalRecord medicalRecord;

  @Column(name = "location_mode", length = 32)
  private String locationMode;

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

  public AppointmentType getAppointmentType() {
    return appointmentType;
  }

  public void setAppointmentType(AppointmentType appointmentType) {
    this.appointmentType = appointmentType;
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

  public Doctor getDoctor() {
    return doctor;
  }

  public void setDoctor(Doctor doctor) {
    this.doctor = doctor;
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

  public MedicalRecord getMedicalRecord() {
    return medicalRecord;
  }

  public void setMedicalRecord(MedicalRecord medicalRecord) {
    this.medicalRecord = medicalRecord;
  }

  public String getLocationMode() {
    return locationMode;
  }

  public void setLocationMode(String locationMode) {
    this.locationMode = locationMode;
  }
}
