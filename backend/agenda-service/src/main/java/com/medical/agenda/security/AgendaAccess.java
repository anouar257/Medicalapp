package com.medical.agenda.security;

/** Accès administration plateforme dans l'agenda (JWT pro : ADMIN sans cabinet). */
public final class AgendaAccess {

  private AgendaAccess() {}

  public static boolean isPlatformAdmin(AgendaProPrincipal p) {
    return p != null && "ADMIN".equals(p.role()) && p.organizationId() == null;
  }
}
