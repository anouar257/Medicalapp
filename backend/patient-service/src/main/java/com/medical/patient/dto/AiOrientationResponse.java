package com.medical.patient.dto;

import java.util.List;

/**
 * Response payload for the medical orientation assistant.
 */
public record AiOrientationResponse(
    String message,
    List<SpecialtyRecommendation> specialties,
    String urgency,
    String warning,
    Boolean needMoreInfo
) {}
