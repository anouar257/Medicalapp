package com.medical.patient.dto;

/**
 * Represents a medical specialty fetched from practitioner-service.
 */
public record SpecialtyDTO(
    Long id,
    String code,
    String libelle
) {}
