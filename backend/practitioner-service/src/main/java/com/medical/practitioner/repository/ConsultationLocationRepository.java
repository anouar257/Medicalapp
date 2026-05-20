package com.medical.practitioner.repository;

import com.medical.practitioner.entity.ConsultationLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationLocationRepository extends JpaRepository<ConsultationLocation, Long> {

    List<ConsultationLocation> findByPractitionerId(Long practitionerId);

    List<ConsultationLocation> findByPractitionerIdAndActifTrue(Long practitionerId);
}
