package com.medical.practitioner.entity;

/**
 * Étape du questionnaire de prise de rendez-vous (cahier des charges « RENDEZ-VOUS »).
 */
public enum SpecialtyBookingStep {
  /** « Avez-vous déjà consulté ce soignant ? » — réponses dépendantes de la spécialité. */
  PRIOR_CARE,
  /** « Motif de la consultation / visite ? » */
  VISIT_REASON
}
