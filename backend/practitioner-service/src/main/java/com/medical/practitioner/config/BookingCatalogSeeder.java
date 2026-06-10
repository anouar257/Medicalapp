package com.medical.practitioner.config;

import com.medical.practitioner.entity.SpecialtyBookingChoice;
import com.medical.practitioner.entity.SpecialtyBookingStep;
import com.medical.practitioner.repository.SpecialtyBookingChoiceRepository;
import com.medical.practitioner.repository.SpecialtyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Données métier « questionnaire RDV » par spécialité (tables {@code specialty_booking_choices}).
 *
 * <p>Idempotent : n’insère que si aucune ligne n’existe encore pour la spécialité cible.
 */
@Configuration
public class BookingCatalogSeeder {

  private static final Logger log = LoggerFactory.getLogger(BookingCatalogSeeder.class);
  private static final String LABEL_CONSULTATION = "Consultation";
  private static final String TYPE_SUIVI = "SUIVI";

  private record Row(SpecialtyBookingStep step, String code, String labelFr, int sort) {}

  @Bean
  CommandLineRunner seedSpecialtyBookingChoices(
      SpecialtyRepository specialtyRepository, SpecialtyBookingChoiceRepository choiceRepository) {
    return args -> {
      int added = 0;
      added += seedForSpecialty(specialtyRepository, choiceRepository, "DEFAULT", defaultDentalLike());
      added += seedForSpecialty(specialtyRepository, choiceRepository, "MEDECINE_GENERALE", defaultDentalLike());
      added += seedForSpecialty(specialtyRepository, choiceRepository, "CHIRURGIE_DENTAIRE", dentistRows());
      added += seedForSpecialty(specialtyRepository, choiceRepository, "AUDIOPROTHESE", audioRows());
      if (added > 0) {
        log.info("[BookingCatalog] {} jeux d’options questionnaire insérés", added);
      }
    };
  }

  private static int seedForSpecialty(
      SpecialtyRepository specialtyRepository,
      SpecialtyBookingChoiceRepository choiceRepository,
      String code,
      Row[] rows) {
    return specialtyRepository
        .findByCodeIgnoreCase(code)
        .map(
            spec -> {
              if (!choiceRepository.findBySpecialtyCode(spec.getCode()).isEmpty()) {
                return 0;
              }
              for (Row r : rows) {
                SpecialtyBookingChoice c = new SpecialtyBookingChoice();
                c.setSpecialty(spec);
                c.setStep(r.step());
                c.setCode(r.code());
                c.setLabelFr(r.labelFr());
                c.setSortOrder(r.sort());
                choiceRepository.save(c);
              }
              return 1;
            })
        .orElse(0);
  }

  private static Row[] defaultDentalLike() {
    return new Row[] {
      new Row(SpecialtyBookingStep.PRIOR_CARE, "NON", "Non, première fois", 10),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "OUI", "Oui, déjà suivi(e)", 20),
      new Row(SpecialtyBookingStep.VISIT_REASON, "CONSULTATION", LABEL_CONSULTATION, 10),
      new Row(SpecialtyBookingStep.VISIT_REASON, TYPE_SUIVI, "Suivi / contrôle", 20),
      new Row(SpecialtyBookingStep.VISIT_REASON, "URGENCE", "Douleur / urgence", 30),
    };
  }

  private static Row[] dentistRows() {
    return new Row[] {
      new Row(SpecialtyBookingStep.PRIOR_CARE, "NON", "Non", 10),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "CONTROLE_ANNUEL", "Contrôle annuel", 20),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "DETARTRAGE", "Détartrage", 30),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "BLANCHIMENT", "Blanchiment", 40),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "CONSULT", LABEL_CONSULTATION, 50),
      new Row(SpecialtyBookingStep.VISIT_REASON, "CONSULTATION", LABEL_CONSULTATION, 10),
      new Row(SpecialtyBookingStep.VISIT_REASON, "DOULEUR", "Douleur dentaire", 20),
      new Row(SpecialtyBookingStep.VISIT_REASON, "CASSURE", "Couronne / bridge / cassure", 30),
      new Row(SpecialtyBookingStep.VISIT_REASON, TYPE_SUIVI, "Suivi post-traitement", 40),
    };
  }

  private static Row[] audioRows() {
    return new Row[] {
      new Row(SpecialtyBookingStep.PRIOR_CARE, "NON", "Non, première fois", 10),
      new Row(SpecialtyBookingStep.PRIOR_CARE, "OUI", "Oui, patient connu", 20),
      new Row(SpecialtyBookingStep.VISIT_REASON, "PREMIERE", "Première consultation", 10),
      new Row(SpecialtyBookingStep.VISIT_REASON, "BILAN", "Bilan auditif", 20),
      new Row(SpecialtyBookingStep.VISIT_REASON, TYPE_SUIVI, "Consultation de suivi", 30),
      new Row(SpecialtyBookingStep.VISIT_REASON, "REGLAGE", "Consultation de réglage", 40),
      new Row(SpecialtyBookingStep.VISIT_REASON, "PANNE", "Audioprothèse en panne", 50),
      new Row(SpecialtyBookingStep.VISIT_REASON, "APPAREIL", "Appareillage / renouvellement", 60),
    };
  }
}
