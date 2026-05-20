package com.medical.practitioner.repository;

import com.medical.practitioner.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findByCode(String code);

    Optional<Specialty> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);
}
