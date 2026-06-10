package com.medical.practitioner.config;

import com.medical.practitioner.entity.Specialty;
import com.medical.practitioner.entity.SpecialtyBookingChoice;
import com.medical.practitioner.entity.SpecialtyBookingStep;
import com.medical.practitioner.repository.SpecialtyBookingChoiceRepository;
import com.medical.practitioner.repository.SpecialtyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Initialise le catalogue de spécialités médicales (référentiel métier) si la table est vide. */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<Specialty> CATALOGUE = List.of(
            new Specialty("MEDECINE_GENERALE", "Médecine générale"),
            new Specialty("CARDIOLOGIE", "Cardiologie"),
            new Specialty("DERMATOLOGIE", "Dermatologie"),
            new Specialty("PEDIATRIE", "Pédiatrie"),
            new Specialty("GYNECOLOGIE", "Gynécologie"),
            new Specialty("OPHTALMOLOGIE", "Ophtalmologie"),
            new Specialty("ORL", "ORL (Oto-rhino-laryngologie)"),
            new Specialty("PSYCHIATRIE", "Psychiatrie"),
            new Specialty("RADIOLOGIE", "Radiologie"),
            new Specialty("NEUROLOGIE", "Neurologie"),
            new Specialty("RHUMATOLOGIE", "Rhumatologie"),
            new Specialty("UROLOGIE", "Urologie"),
            new Specialty("ENDOCRINOLOGIE", "Endocrinologie"),
            new Specialty("GASTROENTEROLOGIE", "Gastro-entérologie"),
            new Specialty("PNEUMOLOGIE", "Pneumologie"),
            new Specialty("CHIRURGIE_GENERALE", "Chirurgie générale"),
            new Specialty("CHIRURGIE_DENTAIRE", "Chirurgie dentaire"),
            new Specialty("ORTHODONTIE", "Orthodontie"),
            new Specialty("KINESITHERAPIE", "Kinésithérapie"),
            new Specialty("OSTEOPATHIE", "Ostéopathie"),
            new Specialty("SAGE_FEMME", "Sage-femme"),
            new Specialty("INFIRMIER", "Infirmier(ère)"),
            new Specialty("DEFAULT", "Générique (questionnaires par défaut)"),
            new Specialty("AUDIOPROTHESE", "Audioprothèse")
    );

    @Bean
    CommandLineRunner seedSpecialties(
            SpecialtyRepository repository,
            SpecialtyBookingChoiceRepository choiceRepository) {
        return args -> {
            seedSpecialtyCatalogue(repository);
            seedBookingWizardChoices(repository, choiceRepository);
        };
    }

    @Transactional
    public void seedSpecialtyCatalogue(SpecialtyRepository repository) {
        int inserted = 0;
        for (Specialty s : CATALOGUE) {
            if (!repository.existsByCode(s.getCode())) {
                repository.save(new Specialty(s.getCode(), s.getLibelle()));
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("[Specialty] Catalogue initialisé : {} nouvelles spécialités insérées", inserted);
        }
    }

    @Transactional
    public void seedBookingWizardChoices(SpecialtyRepository repository, SpecialtyBookingChoiceRepository choiceRepository) {
        if (choiceRepository.count() > 0) return;

        log.info("[Wizard] Initialisation des questions de réservation par défaut...");

        // Questions pour Médecine Générale et Défaut
        String[] targetCodes = {"MEDECINE_GENERALE", "DEFAULT"};

        for (String specCode : targetCodes) {
            repository.findByCode(specCode).ifPresent(spec -> {
                // Étape 1 : Prior Care (Déjà consulté ?)
                createChoice(choiceRepository, spec, SpecialtyBookingStep.PRIOR_CARE, "OLD", "Oui, j'ai déjà consulté ce cabinet", 1);
                createChoice(choiceRepository, spec, SpecialtyBookingStep.PRIOR_CARE, "NEW", "Non, c'est ma première consultation", 2);

                // Étape 2 : Visit Reason (Motif)
                createChoice(choiceRepository, spec, SpecialtyBookingStep.VISIT_REASON, "FOLLOW_UP", "Consultation de suivi / Contrôle", 1);
                createChoice(choiceRepository, spec, SpecialtyBookingStep.VISIT_REASON, "URGENCY", "Urgence / Douleur aiguë", 2);
                createChoice(choiceRepository, spec, SpecialtyBookingStep.VISIT_REASON, "NEW_PATIENT", "Première consultation (nouveau patient)", 3);
            });
        }

        log.info("[Wizard] Questions initialisées avec succès.");
    }

    private void createChoice(SpecialtyBookingChoiceRepository repo, Specialty spec, SpecialtyBookingStep step, String code, String label, int order) {
        SpecialtyBookingChoice c = new SpecialtyBookingChoice();
        c.setSpecialty(spec);
        c.setStep(step);
        c.setCode(code);
        c.setLabelFr(label);
        c.setSortOrder(order);
        repo.save(c);
    }
}
