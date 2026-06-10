package com.medical.practitioner.repository;

import com.medical.practitioner.entity.PractitionerAct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PractitionerActRepository extends JpaRepository<PractitionerAct, Long> {

    List<PractitionerAct> findByPractitionerId(Long practitionerId);

    @Query("SELECT a FROM PractitionerAct a JOIN FETCH a.practitioner")
    List<PractitionerAct> findAllWithPractitioner();
}
