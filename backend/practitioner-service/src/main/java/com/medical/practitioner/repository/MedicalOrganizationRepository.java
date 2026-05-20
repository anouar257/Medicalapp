package com.medical.practitioner.repository;

import com.medical.practitioner.entity.MedicalOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalOrganizationRepository extends JpaRepository<MedicalOrganization, Long> {

    long countByActifTrue();

    Optional<MedicalOrganization> findByEmail(String email);

    Optional<MedicalOrganization> findBySiret(String siret);

    boolean existsByEmail(String email);

    boolean existsBySiret(String siret);
}
