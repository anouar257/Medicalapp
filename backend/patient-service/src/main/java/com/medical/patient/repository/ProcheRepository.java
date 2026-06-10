package com.medical.patient.repository;

import com.medical.patient.entity.Proche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcheRepository extends JpaRepository<Proche, Long> {

  List<Proche> findByPatientId(Long patientId);

  long countByPatientId(Long patientId);

  /** Vérifie qu'un proche appartient bien au patient donné (messagerie inter-services). */
  boolean existsByIdAndPatientId(Long procheId, Long patientId);
}
