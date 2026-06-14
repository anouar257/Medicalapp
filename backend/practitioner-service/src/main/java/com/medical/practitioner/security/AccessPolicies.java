package com.medical.practitioner.security;

import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.repository.PractitionerProfileRepository;
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

  /**
   * Vérifie que l'acteur est soit admin plateforme, soit le propriétaire direct du profil praticien,
   * soit un assistant du même cabinet.
   */
  public static void requireOwnerOrCabinetMember(ProUser actor, PractitionerProfile target) {
    if (isPlatformAdmin(actor)) return;
    if (actor == null || target == null || target.getProUser() == null) throw forbidden();

    // Le praticien est le propriétaire direct
    if (actor.getId().equals(target.getProUser().getId())) return;

    // L'assistant peut agir sur les praticiens de son cabinet
    if (actor.getRole() == ProUserRole.ASSISTANT) {
      Long actorOrg = organizationIdOrNull(actor);
      Long targetOrg = target.getProUser().getOrganization() != null
          ? target.getProUser().getOrganization().getId() : null;
      if (actorOrg != null && actorOrg.equals(targetOrg)) return;
    }

    throw forbidden();
  }

  /**
   * Variante pour un practitionerId : charge le profil et vérifie.
   */
  public static void requireOwnerOrCabinetMember(ProUser actor, Long practitionerId,
                                                 PractitionerProfileRepository repo) {
    if (isPlatformAdmin(actor)) return;
    PractitionerProfile target = repo.findById(practitionerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));
    requireOwnerOrCabinetMember(actor, target);
  }

  private static ResponseStatusException forbidden() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
  }
}
