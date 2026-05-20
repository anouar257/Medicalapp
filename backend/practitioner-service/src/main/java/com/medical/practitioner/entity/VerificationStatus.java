package com.medical.practitioner.entity;

/**
 * Statut d'une vérification de pièce/document (identité, droit d'exercer, etc.).
 */
public enum VerificationStatus {
    NON_FOURNI,
    EN_ATTENTE,
    VERIFIE,
    REFUSE
}
