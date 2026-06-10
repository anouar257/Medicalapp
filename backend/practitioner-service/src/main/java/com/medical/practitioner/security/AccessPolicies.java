package com.medical.practitioner.security;

import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Règles d'accès : admin plateforme (sans cabinet) vs comptes rattachés à un cabinet. */
public final class AccessPolicies {

  private AccessPolicies() {}

  /** Administrateur plateforme : rôle ADMIN et aucune organisation. */
  public static boolean isPlatformAdmin(ProUser u) {
    return u != null && u.getRole() == ProUserRole.ADMIN && u.getOrganization() == null;
  }

  public static void requirePlatformAdmin(ProUser u) {
    if (!isPlatformAdmin(u)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Accès réservé à l'administration plateforme");
    }
  }

  public static Long organizationIdOrNull(ProUser u) {
    if (u == null || u.getOrganization() == null) {
      return null;
    }
    return u.getOrganization().getId();
  }

  public static void requireReadCabinet(ProUser actor, Long organizationId) {
    if (isPlatformAdmin(actor)) {
      return;
    }
    Long mine = organizationIdOrNull(actor);
    if (mine == null || !mine.equals(organizationId)) {
      throw forbidden();
    }
  }

  /** Plateforme ou praticien du cabinet concerné. */
  public static void requirePlatformAdminOrCabinetPraticien(ProUser actor, Long organizationId) {
    if (isPlatformAdmin(actor)) {
      return;
    }
    if (actor == null) {
      throw forbidden();
    }
    if (actor.getRole() != ProUserRole.PRATICIEN) {
      throw forbidden();
    }
    Long mine = organizationIdOrNull(actor);
    if (mine == null || !mine.equals(organizationId)) {
      throw forbidden();
    }
  }

  private static ResponseStatusException forbidden() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
  }
}
