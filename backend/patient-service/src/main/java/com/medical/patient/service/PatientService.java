package com.medical.patient.service;

import com.medical.patient.entity.Patient;
import com.medical.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service métier Patient — gestion du profil.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Optional<Patient> findById(Long id) {
        return patientRepository.findById(id);
    }

    public Optional<Patient> findByEmail(String email) {
        return patientRepository.findByEmail(email);
    }

    /**
     * Met à jour le profil du patient (champs modifiables uniquement).
     */
    @Transactional
    public Patient updateProfile(Long patientId, Patient updates) {
        Patient existing = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));

        if (updates.getPrenom() != null) existing.setPrenom(updates.getPrenom());
        if (updates.getNom() != null) existing.setNom(updates.getNom());
        if (updates.getSexe() != null) existing.setSexe(updates.getSexe());
        if (updates.getDateNaissance() != null) existing.setDateNaissance(updates.getDateNaissance());

        return patientRepository.save(existing);
    }

    /**
     * Désactive le compte patient.
     */
    @Transactional
    public void deactivateAccount(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));
        patient.setActif(false);
        patientRepository.save(patient);
    }
}
