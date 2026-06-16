package com.medical.agenda.dto;

/**
 * Represent an appointment state change event sent over Kafka.
 */
public record AppointmentEvent(
    Long appointmentId,
    Long patientId,
    Long practitionerId,
    Long organizationId,
    String dateTime,
    String status,
    Double fee
) {}
