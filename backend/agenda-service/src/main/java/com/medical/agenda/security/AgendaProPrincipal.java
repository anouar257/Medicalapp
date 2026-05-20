package com.medical.agenda.security;

/** Identité extraite du JWT pro (practitioner-service) — pas de requête base agenda. */
public record AgendaProPrincipal(Long userId, String email, String role, Long organizationId) {}
