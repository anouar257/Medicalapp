package com.medical.practitioner.repository;

import com.medical.practitioner.entity.Diploma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiplomaRepository extends JpaRepository<Diploma, Long> {

    List<Diploma> findByPractitionerId(Long practitionerId);
}
