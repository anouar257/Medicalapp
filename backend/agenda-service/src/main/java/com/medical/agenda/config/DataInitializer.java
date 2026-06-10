package com.medical.agenda.config;

import com.medical.agenda.entity.Doctor;
import com.medical.agenda.repository.DoctorRepository;
import com.medical.agenda.service.DoctorService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialisation technique du module agenda : compléments non destructifs sur les données
 * existantes (schéma, photos, spécialités manquantes).
 *
 * <p>Aucun type générique n'est recréé ici : le catalogue doit désormais provenir uniquement des
 * actes médicaux synchronisés depuis les profils praticiens.
 */
@Configuration
public class DataInitializer {

  /** Renseigne une spécialité par défaut si la colonne existe mais est vide (migration). */
  @Transactional
  public void backfillDoctorSpecialty(DoctorRepository doctorRepository) {
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

  @Bean
  CommandLineRunner seedAgenda(JdbcTemplate jdbcTemplate, DoctorRepository doctorRepository) {
    return args -> {
      ensureAppointmentTypesSchema(jdbcTemplate);
      removeUnreferencedLegacyAppointmentTypes(jdbcTemplate);
      backfillDoctorPhotos(doctorRepository);
      backfillDoctorSpecialty(doctorRepository);
    };
  }

  /**
   * Les vieux volumes Docker conservent parfois une table {@code appointment_types} incomplète.
   * On ajoute ici les colonnes attendues par la version courante avant les premières requêtes métier.
   */
  public void ensureAppointmentTypesSchema(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ADD COLUMN IF NOT EXISTS price NUMERIC(10,2)");
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ADD COLUMN IF NOT EXISTS price_variable BOOLEAN DEFAULT FALSE");
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ADD COLUMN IF NOT EXISTS source_practitioner_id BIGINT");
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ADD COLUMN IF NOT EXISTS source_act_id BIGINT");
    jdbcTemplate.execute(
        "UPDATE appointment_types SET price_variable = FALSE WHERE price_variable IS NULL");
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ALTER COLUMN price_variable SET DEFAULT FALSE");
    jdbcTemplate.execute(
        "ALTER TABLE appointment_types ALTER COLUMN price_variable SET NOT NULL");
  }

  /**
   * Nettoie les anciens types génériques non référencés. Les types encore utilisés seront remappés
   * progressivement lors de la synchronisation des actes puis supprimés dès qu'ils deviennent orphelins.
   */
  public void removeUnreferencedLegacyAppointmentTypes(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(
        """
        DELETE FROM appointment_types t
        WHERE t.source_act_id IS NULL
          AND t.source_practitioner_id IS NULL
          AND NOT EXISTS (
            SELECT 1
            FROM appointments a
            WHERE a.appointment_type_id = t.id
          )
        """);
  }

  /** Remplit la photo des médecins existants qui n'en ont pas. */
  @Transactional
  public void backfillDoctorPhotos(DoctorRepository doctorRepository) {
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
