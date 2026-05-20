package com.medical.agenda.security;

/** Identité extraite du JWT patient (patient-service). */
public record AgendaPatientPrincipal(Long patientId) {}
