package com.medical.agenda.repository;

import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  long countByDoctorId(Long doctorId);

  @Query("SELECT a.doctor.id, COUNT(a) FROM Appointment a GROUP BY a.doctor.id")
  List<Object[]> countAppointmentsGroupedByDoctorId();

  /**
   * Rendez-vous dont le début est dans l'intervalle [start, end] (comportement Spring Data
   * {@code Between} inclusif des deux côtés selon la version — pour la grille on préfère le
   * chevauchement d'intervalle, voir {@link #findOverlappingRange}).
   */
  List<Appointment> findByStartTimeBetween(Instant start, Instant end);

  /**
   * Tous les RDV qui intersectent la fenêtre temporelle [rangeStart, rangeEnd) (vue calendrier).
   */
  @Query(
      "SELECT a FROM Appointment a WHERE a.startTime < :rangeEnd AND a.endTime > :rangeStart AND a.status IN (com.medical.agenda.entity.AppointmentStatus.CONFIRMED, com.medical.agenda.entity.AppointmentStatus.COMPLETED, com.medical.agenda.entity.AppointmentStatus.NO_SHOW) ORDER BY a.startTime ASC")
  List<Appointment> findOverlappingRange(
      @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);

  /** Cloisonnement multi-cabinets : RDV qui intersectent la fenêtre et dont le médecin est rattaché au cabinet. */
  @Query(
      "SELECT a FROM Appointment a WHERE a.startTime < :rangeEnd AND a.endTime > :rangeStart "
          + "AND a.doctor.organizationId = :organizationId "
          + "AND a.status IN (com.medical.agenda.entity.AppointmentStatus.CONFIRMED, com.medical.agenda.entity.AppointmentStatus.COMPLETED, com.medical.agenda.entity.AppointmentStatus.NO_SHOW) ORDER BY a.startTime ASC")
  List<Appointment> findOverlappingRangeForCabinet(
      @Param("rangeStart") Instant rangeStart,
      @Param("rangeEnd") Instant rangeEnd,
      @Param("organizationId") Long organizationId);

  /** Rendez-vous d'un patient (décroissant par date de début). */
  List<Appointment> findByPatientIdOrderByStartTimeDesc(Long patientId);

  /**
   * Vue patient en lecture seule, via projection native pour tolérer les anciennes lignes
   * incomplètes (statut ou jointures manquants) sans faire tomber tout le endpoint.
   */
  @Query(
      value =
          """
          SELECT
            a.id,
            a.title,
            a.patient_id,
            a.patient_prenom,
            a.patient_nom,
            a.visit_reason_code,
            a.appointment_type_id,
            t.code,
            t.label,
            t.color_code,
            a.start_time,
            a.end_time,
            a.duration_minutes,
            a.description,
            a.doctor_id,
            a.color,
            d.name,
            d.specialty,
            d.external_practitioner_id,
            a.status,
            a.location_mode
          FROM appointments a
          LEFT JOIN appointment_types t ON t.id = a.appointment_type_id
          LEFT JOIN doctors d ON d.id = a.doctor_id
          WHERE a.patient_id = :patientId
          ORDER BY a.start_time DESC
          """,
      nativeQuery = true)
  List<Object[]> findPatientAppointmentRows(@Param("patientId") Long patientId);

  @Query(
      """
      SELECT a FROM Appointment a
      WHERE a.patientId = :patientId
        AND a.doctor.organizationId = :organizationId
        AND (a.status IS NULL OR a.status <> com.medical.agenda.entity.AppointmentStatus.CANCELLED)
      ORDER BY a.startTime DESC
      """)
  List<Appointment> findByPatientIdAndDoctorOrganizationId(
      @Param("patientId") Long patientId, @Param("organizationId") Long organizationId);

  @Query(
      """
      SELECT COUNT(a) > 0 FROM Appointment a
      WHERE a.doctor.id = :doctorId
        AND (a.status IS NULL OR a.status IN (com.medical.agenda.entity.AppointmentStatus.CONFIRMED, com.medical.agenda.entity.AppointmentStatus.COMPLETED, com.medical.agenda.entity.AppointmentStatus.NO_SHOW))
        AND a.startTime < :end
        AND a.endTime > :start
      """)
  boolean existsBlockingOverlap(
      @Param("doctorId") Long doctorId,
      @Param("start") Instant start,
      @Param("end") Instant end);

  List<Appointment> findByDoctorOrganizationIdAndStatusOrderByStartTimeAsc(
      Long organizationId, AppointmentStatus status);

  List<Appointment> findByStatusOrderByStartTimeAsc(AppointmentStatus status);

  /**
   * Rendez-vous non annulés entre le patient et le praticien (profil practitioner, {@code
   * Doctor.externalPractitionerId}) — utilisé par messaging-service (inter-service).
   */
  @Query(
      """
      SELECT COUNT(a) FROM Appointment a
      WHERE a.patientId = :patientId
        AND a.doctor.externalPractitionerId = :externalPractitionerId
        AND (a.status IS NULL OR a.status <> com.medical.agenda.entity.AppointmentStatus.CANCELLED)
      """)
  long countActiveRelationBetweenPatientAndPractitionerProfile(
      @Param("patientId") Long patientId,
      @Param("externalPractitionerId") Long externalPractitionerId);

  @Query(
      """
      SELECT COUNT(a) FROM Appointment a
      WHERE a.patientId = :patientId
        AND a.doctor.externalPractitionerId = :externalPractitionerId
        AND a.status = com.medical.agenda.entity.AppointmentStatus.COMPLETED
      """)
  long countCompletedRelationBetweenPatientAndPractitionerProfile(
      @Param("patientId") Long patientId,
      @Param("externalPractitionerId") Long externalPractitionerId);

  /**
   * Remappe les anciens rendez-vous encore liés à un type générique vers le type dynamique issu du
   * profil praticien, en s'appuyant sur le motif de visite déjà stocké en base.
   */
  @Modifying
  @Query(
      value =
          """
          UPDATE appointments a
          SET appointment_type_id = :appointmentTypeId
          FROM doctors d
          WHERE a.doctor_id = d.id
            AND d.external_practitioner_id = :sourcePractitionerId
            AND LOWER(BTRIM(COALESCE(a.visit_reason_code, ''))) = LOWER(BTRIM(:typeLabel))
            AND (
              a.appointment_type_id IS NULL
              OR EXISTS (
                SELECT 1
                FROM appointment_types current_type
                WHERE current_type.id = a.appointment_type_id
                  AND (current_type.source_act_id IS NULL OR current_type.source_practitioner_id IS NULL)
              )
            )
          """,
      nativeQuery = true)
  int rebindLegacyAppointmentsToDynamicType(
      @Param("appointmentTypeId") Long appointmentTypeId,
      @Param("sourcePractitionerId") Long sourcePractitionerId,
      @Param("typeLabel") String typeLabel);

  @Query(
      """
      SELECT a FROM Appointment a
      JOIN FETCH a.doctor d
      LEFT JOIN FETCH a.appointmentType t
      WHERE d.externalPractitionerId = :sourcePractitionerId
        AND (t IS NULL OR t.sourceActId IS NULL OR t.sourcePractitionerId IS NULL)
      ORDER BY a.startTime ASC
      """)
  List<Appointment> findLegacyAppointmentsForPractitioner(
      @Param("sourcePractitionerId") Long sourcePractitionerId);

  @Query(
      """
      SELECT a FROM Appointment a
      JOIN FETCH a.doctor d
      LEFT JOIN FETCH a.appointmentType t
      WHERE d.externalPractitionerId = :sourcePractitionerId
      ORDER BY a.startTime ASC
      """)
  List<Appointment> findAppointmentsForPractitioner(
      @Param("sourcePractitionerId") Long sourcePractitionerId);
}
