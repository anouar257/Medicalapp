package com.medical.practitioner.config;

import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.repository.ProUserRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Compte maître plateforme (email configurable) : rôle {@code ADMIN}, sans organisation.
 */
@Configuration
public class PlatformAdminBootstrap {

  private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

  /** Téléphone technique unique (contrainte BDD), non utilisé pour l'authentification. */
  private static final String PLATFORM_ADMIN_PHONE = "PLATFORM-ADMIN-0000000001";

  @Bean
  @Order(10)
  CommandLineRunner seedPlatformAdmin(
      ProUserRepository proUserRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.platform-admin.email:anouarmnt@gmail.com}") String email,
      @Value("${app.platform-admin.initial-password:ChangeMe!PlatformAdmin2026}") String initialPassword) {
    return args -> {
      String normalized = email.trim().toLowerCase();
      Optional<ProUser> existing = proUserRepository.findByEmail(normalized);
      if (existing.isPresent()) {
        ProUser u = existing.get();
        boolean dirty = false;
        if (u.getRole() != ProUserRole.ADMIN) {
          u.setRole(ProUserRole.ADMIN);
          dirty = true;
        }
        if (u.getOrganization() != null) {
          u.setOrganization(null);
          dirty = true;
        }
        if (u.getTelephone() == null || !PLATFORM_ADMIN_PHONE.equals(u.getTelephone())) {
          u.setTelephone(PLATFORM_ADMIN_PHONE);
          dirty = true;
        }
        if (dirty) {
          proUserRepository.save(u);
          log.warn(
              "[PlatformAdmin] Compte {} aligné sur ADMIN plateforme (sans organisation).",
              normalized);
        }
        return;
      }

      ProUser u = new ProUser();
      u.setEmail(normalized);
      u.setTelephone(PLATFORM_ADMIN_PHONE);
      u.setMotDePasse(passwordEncoder.encode(initialPassword));
      u.setPrenom("Administration");
      u.setNom("Plateforme");
      u.setRole(ProUserRole.ADMIN);
      u.setOrganization(null);
      u.setCguAcceptees(true);
      u.setEmailVerifie(true);
      u.setTelephoneVerifie(false);
      u.setActif(true);
      u.setDateInscription(Instant.now());
      proUserRepository.save(u);
      log.warn(
          "[PlatformAdmin] Compte maître créé (email={}). Définissez app.platform-admin.initial-password en production.",
          normalized);
    };
  }
}
