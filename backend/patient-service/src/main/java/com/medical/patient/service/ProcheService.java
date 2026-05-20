package com.medical.patient.service;

import com.medical.patient.entity.Patient;
import com.medical.patient.entity.Proche;
import com.medical.patient.repository.PatientRepository;
import com.medical.patient.repository.ProcheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service métier Proche — gestion des proches d'un patient.
 */
@Service
public class ProcheService {

    private static final Logger log = LoggerFactory.getLogger(ProcheService.class);

    private final ProcheRepository procheRepository;
    private final PatientRepository patientRepository;

    public ProcheService(ProcheRepository procheRepository, PatientRepository patientRepository) {
        this.procheRepository = procheRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Récupère tous les proches d'un patient.
     */
    public List<Proche> findByPatientId(Long patientId) {
        return procheRepository.findByPatientId(patientId);
    }

    /**
     * Récupère un proche par son ID (vérifie qu'il appartient au patient).
     */
    public Proche findByIdAndPatientId(Long procheId, Long patientId) {
        Proche proche = procheRepository.findById(procheId)
                .orElseThrow(() -> new IllegalArgumentException("Proche non trouvé"));
        if (!proche.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("Ce proche n'appartient pas à votre compte");
        }
        return proche;
    }

    /**
     * Crée un nouveau proche rattaché au patient authentifié.
     */
    @Transactional
    public Proche create(Long patientId, Proche proche) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient non trouvé"));

        proche.setPatient(patient);
        proche.setDateCreation(Instant.now());

        // Logique conditionnelle : si nomFamilleChange est false, on efface l'ancien nom
        if (!proche.isNomFamilleChange()) {
            proche.setAncienNomFamille(null);
        }

        Proche saved = procheRepository.save(proche);
        log.info("Proche créé — ID: {}, rattaché au patient ID: {}", saved.getId(), patientId);
        return saved;
    }

    /**
     * Met à jour un proche existant.
     */
    @Transactional
    public Proche update(Long procheId, Long patientId, Proche updates) {
        Proche existing = findByIdAndPatientId(procheId, patientId);

        existing.setCivilite(updates.getCivilite());
        existing.setSexe(updates.getSexe());
        existing.setPrenom(updates.getPrenom());
        existing.setNom(updates.getNom());
        existing.setNomFamilleChange(updates.isNomFamilleChange());
        existing.setAncienNomFamille(updates.isNomFamilleChange() ? updates.getAncienNomFamille() : null);
        existing.setDateNaissance(updates.getDateNaissance());
        existing.setPaysNaissance(updates.getPaysNaissance());
        existing.setVilleNaissance(updates.getVilleNaissance());
        existing.setTelephoneMobile(updates.getTelephoneMobile());
        existing.setTelephoneFixe(updates.getTelephoneFixe());
        existing.setEmail(updates.getEmail());
        existing.setAdresse(updates.getAdresse());
        existing.setCodePostal(updates.getCodePostal());
        existing.setVille(updates.getVille());
        existing.setAssurance(updates.getAssurance());
        existing.setRemarque(updates.getRemarque());
        existing.setProvenance(updates.getProvenance());
        existing.setProfession(updates.getProfession());
        existing.setMedecinTraitant(updates.getMedecinTraitant());
        existing.setEnvoiSmsActive(updates.isEnvoiSmsActive());
        existing.setEnvoiEmailActive(updates.isEnvoiEmailActive());
        existing.setPieceIdentiteValidee(updates.isPieceIdentiteValidee());
        existing.setIdentiteDouteuse(updates.isIdentiteDouteuse());

        Proche saved = procheRepository.save(existing);
        log.info("Proche mis à jour — ID: {}", saved.getId());
        return saved;
    }

    /**
     * Supprime un proche.
     */
    @Transactional
    public void delete(Long procheId, Long patientId) {
        Proche proche = findByIdAndPatientId(procheId, patientId);
        procheRepository.delete(proche);
        log.info("Proche supprimé — ID: {}", procheId);
    }
}
