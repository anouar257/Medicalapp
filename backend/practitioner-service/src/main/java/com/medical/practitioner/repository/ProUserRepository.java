package com.medical.practitioner.repository;

import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProUserRepository extends JpaRepository<ProUser, Long> {

    Optional<ProUser> findByEmail(String email);

    Optional<ProUser> findByTelephone(String telephone);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    Optional<ProUser> findByResetToken(String resetToken);

    List<ProUser> findByOrganizationId(Long organizationId);

    List<ProUser> findByOrganizationIdAndRole(Long organizationId, ProUserRole role);

    @Query("SELECT u FROM ProUser u JOIN FETCH u.organization WHERE u.id = :id")
    Optional<ProUser> findByIdWithOrganization(@Param("id") Long id);
}
