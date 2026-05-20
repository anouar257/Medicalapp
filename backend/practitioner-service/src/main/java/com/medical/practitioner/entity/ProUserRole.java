package com.medical.practitioner.entity;

/**
 * Rôles professionnels MedConnect (3 comptes métier côté praticien).
 *
 * <p>Le rôle {@link #ADMIN} est réservé au compte plateforme <strong>sans
 * organisation</strong> (tour de contrôle globale). Les comptes cabinet sont
 * {@link #PRATICIEN} ou {@link #ASSISTANT}.
 *
 * <p>Le compte patient (espace santé) est géré par le patient-service.
 */
public enum ProUserRole {

  /** Administration plateforme — aucun cabinet rattaché ({@code organization_id} null). */
  ADMIN,

  /** Médecin ou responsable de cabinet — agenda et gestion des assistants. */
  PRATICIEN,

  /** Assistant — agenda et validation des demandes de rendez-vous. */
  ASSISTANT
}
