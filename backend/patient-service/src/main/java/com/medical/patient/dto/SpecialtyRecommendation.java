package com.medical.patient.dto;

/**
 * Represents a medical specialty recommended by the AI.
 */
public record SpecialtyRecommendation(
    Long id,
    String code,
    String label
) {}
