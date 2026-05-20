package com.medical.practitioner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migre les anciennes valeurs de colonne {@code role} vers le modèle à 3 rôles pro
 * (ADMIN plateforme, PRATICIEN, ASSISTANT) avant toute lecture JPA.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyProRoleMigration implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(LegacyProRoleMigration.class);

  private final JdbcTemplate jdbcTemplate;

  public LegacyProRoleMigration(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      int n1 = jdbcTemplate.update("UPDATE pro_users SET role = 'ADMIN' WHERE role = 'SUPER_ADMIN'");
      int n2 =
          jdbcTemplate.update(
              "UPDATE pro_users SET role = 'PRATICIEN' WHERE role = 'ADMIN' AND organization_id IS NOT NULL");
      int n3 =
          jdbcTemplate.update(
              "UPDATE pro_users SET role = 'PRATICIEN' WHERE role IN ("
                  + "'SECRETAIRE','PERSONNEL_ADMINISTRATIF','RESPONSABLE_OPERATIONNEL',"
                  + "'REGULATEUR','AGENT_PARTENAIRE','AUTRE')");
      if (n1 + n2 + n3 > 0) {
        log.info("[Migration] Rôles pro normalisés : SUPER_ADMIN→ADMIN: {}, anciens ADMIN cabinet→PRATICIEN: {}, autres→PRATICIEN: {}", n1, n2, n3);
      }
    } catch (Exception e) {
      log.warn("[Migration] Impossible de normaliser les rôles (base vide ou schéma différent) : {}", e.getMessage());
    }
  }
}
