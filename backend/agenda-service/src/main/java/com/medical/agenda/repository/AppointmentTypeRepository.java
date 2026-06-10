package com.medical.agenda.repository;

import com.medical.agenda.entity.AppointmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {

  Optional<AppointmentType> findByCode(String code);

  List<AppointmentType> findAllByOrderByDisplayOrderAscIdAsc();

  List<AppointmentType> findByActiveTrueOrderByDisplayOrderAsc();

  List<AppointmentType> findBySourcePractitionerIdOrderByDisplayOrderAscIdAsc(Long sourcePractitionerId);

  boolean existsByCode(String code);

  @Modifying
  @Query(
      value =
          """
          DELETE FROM appointment_types t
          WHERE t.source_act_id IS NULL
            AND t.source_practitioner_id IS NULL
            AND NOT EXISTS (
              SELECT 1
              FROM appointments a
              WHERE a.appointment_type_id = t.id
            )
          """,
      nativeQuery = true)
  int deleteUnreferencedLegacyTypes();
}
