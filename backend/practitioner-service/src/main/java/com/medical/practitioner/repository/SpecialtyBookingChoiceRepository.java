package com.medical.practitioner.repository;

import com.medical.practitioner.entity.SpecialtyBookingChoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecialtyBookingChoiceRepository extends JpaRepository<SpecialtyBookingChoice, Long> {

  @Query(
      """
      SELECT c FROM SpecialtyBookingChoice c
      JOIN FETCH c.specialty s
      WHERE UPPER(s.code) = UPPER(:code)
      ORDER BY c.step ASC, c.sortOrder ASC, c.id ASC
      """)
  List<SpecialtyBookingChoice> findBySpecialtyCode(@Param("code") String code);
}
