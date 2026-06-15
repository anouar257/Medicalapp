package com.medical.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for the medical orientation assistant.
 */
public record AiOrientationRequest(
    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(min = 2, max = 500, message = "Le message doit contenir entre 2 et 500 caractères")
    String message,

    @Pattern(regexp = "^(fr|en|ar)$", message = "La langue doit être fr, en ou ar")
    String language,

    List<ChatMessage> history
) {}
