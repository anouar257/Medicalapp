package com.medical.patient.dto;

import java.util.List;

/**
 * Payload sent to the Python ai-service including the database specialties catalog.
 */
public record AiServiceRequest(
    String message,
    String language,
    List<ChatMessage> history,
    List<SpecialtyDTO> specialties
) {}
