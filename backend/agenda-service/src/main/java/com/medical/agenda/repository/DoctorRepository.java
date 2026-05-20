package com.medical.agenda.repository;

import com.medical.agenda.entity.Doctor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

  /**
   * Recherche un médecin par son identifiant externe (ID du PractitionerProfile dans le
   * practitioner-service). Utilisé par le endpoint de synchronisation.
   */
  Optional<Doctor> findByExternalPractitionerId(Long externalPractitionerId);
  long countByExternalPractitionerIdIsNotNull();

  /** Médecins rattachés à un cabinet (cloisonnement multi-cabinets de l'agenda). */
  List<Doctor> findByOrganizationId(Long organizationId);
}

