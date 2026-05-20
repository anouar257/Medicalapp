package com.medical.agenda.repository;

import com.medical.agenda.entity.Appointment;
import com.medical.agenda.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  long countByDoctor_Id(Long doctorId);

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
      "SELECT a FROM Appointment a WHERE a.startTime < :rangeEnd AND a.endTime > :rangeStart AND a.status = 'CONFIRMED' ORDER BY a.startTime ASC")
  List<Appointment> findOverlappingRange(
      @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);

  /** Cloisonnement multi-cabinets : RDV qui intersectent la fenêtre et dont le médecin est rattaché au cabinet. */
  @Query(
      "SELECT a FROM Appointment a WHERE a.startTime < :rangeEnd AND a.endTime > :rangeStart "
          + "AND a.doctor.organizationId = :organizationId "
          + "AND a.status = 'CONFIRMED' ORDER BY a.startTime ASC")
  List<Appointment> findOverlappingRangeForCabinet(
      @Param("rangeStart") Instant rangeStart,
      @Param("rangeEnd") Instant rangeEnd,
      @Param("organizationId") Long organizationId);

  /** Rendez-vous d'un patient (décroissant par date de début). */
  List<Appointment> findByPatientIdOrderByStartTimeDesc(Long patientId);

  @Query(
      """
      SELECT a FROM Appointment a
      WHERE a.patientId = :patientId
        AND a.doctor.organizationId = :organizationId
        AND (a.status IS NULL OR a.status <> 'CANCELLED')
      ORDER BY a.startTime DESC
      """)
  List<Appointment> findByPatientIdAndDoctorOrganizationId(
      @Param("patientId") Long patientId, @Param("organizationId") Long organizationId);

  @Query(
      """
      SELECT COUNT(a) > 0 FROM Appointment a
      WHERE a.doctor.id = :doctorId
        AND (a.status IS NULL OR a.status <> 'CANCELLED')
        AND a.startTime < :end
        AND a.endTime > :start
      """)
  boolean existsBlockingOverlap(
      @Param("doctorId") Long doctorId,
      @Param("start") Instant start,
      @Param("end") Instant end);

  List<Appointment> findByDoctor_OrganizationIdAndStatusOrderByStartTimeAsc(
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
        AND (a.status IS NULL OR a.status <> 'CANCELLED')
      """)
  long countActiveRelationBetweenPatientAndPractitionerProfile(
      @Param("patientId") Long patientId,
      @Param("externalPractitionerId") Long externalPractitionerId);
}
