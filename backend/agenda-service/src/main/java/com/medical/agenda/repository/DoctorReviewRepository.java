package com.medical.agenda.repository;

import com.medical.agenda.entity.DoctorReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {

  boolean existsByAppointmentId(Long appointmentId);

  Optional<DoctorReview> findByAppointmentId(Long appointmentId);

  List<DoctorReview> findByExternalPractitionerIdOrderByCreatedAtDesc(Long externalPractitionerId);
}
