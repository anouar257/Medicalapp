package com.medical.practitioner.repository;

import com.medical.practitioner.entity.PractitionerProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PractitionerProfileRepository extends JpaRepository<PractitionerProfile, Long> {

    Optional<PractitionerProfile> findByProUserId(Long proUserId);

    Optional<PractitionerProfile> findByEmpreinte(String empreinte);

    boolean existsByEmpreinte(String empreinte);

    List<PractitionerProfile> findByProUserOrganizationId(Long organizationId);

  @Query(
      """
      SELECT DISTINCT p FROM PractitionerProfile p
      JOIN FETCH p.proUser u
      WHERE p.id IN :ids
      """)
  List<PractitionerProfile> findAllByIdInWithProUser(@Param("ids") Collection<Long> ids);

  /**
   * Recherche “public” pour la Landing Page.
   *
   * <p>Filtres optionnels gérés via {@code LIKE %...%} :
   * <ul>
   *   <li>{@code name} : prénom / nom</li>
   *   <li>{@code ville} : ville du lieu de consultation</li>
   *   <li>{@code specialty} : libellé (ou code) de la spécialité</li>
   * </ul>
   */
  @Query("""
      SELECT DISTINCT p
      FROM PractitionerProfile p
      JOIN FETCH p.proUser u
      LEFT JOIN FETCH u.organization o
      LEFT JOIN p.specialites s
      LEFT JOIN p.lieuxConsultation l
      WHERE p.disponible = true
        AND (:name IS NULL OR LOWER(u.nom) LIKE :name OR LOWER(u.prenom) LIKE :name)
        AND (:ville IS NULL OR LOWER(l.ville) LIKE :ville OR LOWER(o.ville) LIKE :ville)
        AND (
             :specialty IS NULL
             OR LOWER(s.libelle) LIKE :specialty
             OR LOWER(s.code) LIKE :specialty
        )
      """)
  List<PractitionerProfile> searchPublic(
      @Param("name") String name,
      @Param("ville") String ville,
      @Param("specialty") String specialty);
}
