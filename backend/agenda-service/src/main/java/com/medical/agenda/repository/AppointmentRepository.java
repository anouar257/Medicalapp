package com.medical.agenda.repository;

import com.medical.agenda.entity.Appointment;
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
      "SELECT a FROM Appointment a WHERE a.startTime < :rangeEnd AND a.endTime > :rangeStart ORDER BY a.startTime ASC")
  List<Appointment> findOverlappingRange(
      @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);
}
