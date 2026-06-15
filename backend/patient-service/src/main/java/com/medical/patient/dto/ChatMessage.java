package com.medical.patient.dto;

/**
 * Represents a single chat message in the conversation history.
 */
public record ChatMessage(
    String role,
    String content
) {}
