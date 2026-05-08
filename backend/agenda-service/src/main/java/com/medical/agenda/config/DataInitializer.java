package com.medical.agenda.config;

import com.medical.agenda.entity.AppointmentType;
import com.medical.agenda.entity.Doctor;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.AppointmentTypeRepository;
import com.medical.agenda.repository.DoctorRepository;
import com.medical.agenda.service.DoctorService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialisation cabinet : synchronisation du catalogue officiel des types de visite + compléments
 * (photos médecins, FK legacy sur anciens RDV). Aucun médecin ni rendez-vous injectés — tout passe
 * par l’admin ou l’agenda.
 */
@Configuration
public class DataInitializer {

  /**
   * Les trois types « métier » du cabinet — libellés FR, couleurs et ordre d’affichage alignés
   * sidebar / formulaires (source unique ici, pas de jeu de données fictif).
   */
  private static final CatalogType[] VISIT_TYPES = {
    new CatalogType("CONSULTATION", "Consultation", "#ef4444", 15, 10),
    new CatalogType("SURGERY", "Chirurgie", "#a855f7", 45, 15),
    new CatalogType("CONTROL", "Contrôle", "#0284c7", 5, 20),
  };

  /** Renseigne une spécialité par défaut si la colonne existe mais est vide (migration). */
  @Transactional
  void backfillDoctorSpecialty(DoctorRepository doctorRepository) {
    List<Doctor> docs = doctorRepository.findAll();
    boolean dirty = false;
    for (Doctor d : docs) {
      if (d.getSpecialty() == null || d.getSpecialty().isBlank()) {
        d.setSpecialty("Médecine générale");
        dirty = true;
      }
    }
    if (dirty) {
      doctorRepository.saveAll(docs);
    }
  }

  private record CatalogType(
      String code, String label, String colorCode, int defaultDurationMinutes, int displayOrder) {}

  @Bean
  CommandLineRunner seedAgenda(
      DoctorRepository doctorRepository,
      AppointmentRepository appointmentRepository,
      AppointmentTypeRepository typeRepository) {
    return args -> {
      syncVisitTypeCatalog(typeRepository);
      backfillDoctorPhotos(doctorRepository);
      backfillDoctorSpecialty(doctorRepository);
    };
  }

  /** Insère les trois types canoniques s’ils manquent (sans écraser les réglages admin ultérieurs). */
  @Transactional
  void syncVisitTypeCatalog(AppointmentTypeRepository typeRepository) {
    for (CatalogType def : VISIT_TYPES) {
      if (typeRepository.existsByCode(def.code())) {
        continue;
      }
      AppointmentType t = new AppointmentType();
      t.setCode(def.code());
      t.setLabel(def.label());
      t.setColorCode(def.colorCode());
      t.setDefaultDurationMinutes(def.defaultDurationMinutes());
      t.setDisplayOrder(def.displayOrder());
      t.setActive(true);
      typeRepository.save(t);
    }
  }

  /** Remplit la photo des médecins existants qui n’en ont pas. */
  @Transactional
  void backfillDoctorPhotos(DoctorRepository doctorRepository) {
    List<Doctor> docs = doctorRepository.findAll();
    boolean dirty = false;
    for (Doctor d : docs) {
      if (d.getPhotoUrl() == null || d.getPhotoUrl().isBlank()) {
        d.setPhotoUrl(DoctorService.DEFAULT_DOCTOR_PHOTO_ASSET);
        dirty = true;
      }
    }
    if (dirty) {
      doctorRepository.saveAll(docs);
    }
  }

}
