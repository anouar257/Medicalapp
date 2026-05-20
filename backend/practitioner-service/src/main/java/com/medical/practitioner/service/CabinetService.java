package com.medical.practitioner.service;

import com.medical.practitioner.dto.CreateProUserRequest;
import com.medical.practitioner.dto.MedicalOrganizationDTO;
import com.medical.practitioner.dto.ProUserDTO;
import com.medical.practitioner.entity.MedicalOrganization;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.repository.MedicalOrganizationRepository;
import com.medical.practitioner.repository.ProUserRepository;
import com.medical.practitioner.security.AccessPolicies;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CabinetService {

  private static final Logger log = LoggerFactory.getLogger(CabinetService.class);
  private static final String PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final MedicalOrganizationRepository organizationRepository;
  private final ProUserRepository proUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final OtpService otpService;

  public CabinetService(
      MedicalOrganizationRepository organizationRepository,
      ProUserRepository proUserRepository,
      PasswordEncoder passwordEncoder,
      OtpService otpService) {
    this.organizationRepository = organizationRepository;
    this.proUserRepository = proUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.otpService = otpService;
  }

  @Transactional(readOnly = true)
  public MedicalOrganizationDTO findById(Long id) {
    MedicalOrganization org =
        organizationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cabinet introuvable"));
    return MedicalOrganizationDTO.fromEntity(org);
  }

  @Transactional(readOnly = true)
  public MedicalOrganizationDTO findByIdForActor(ProUser actor, Long id) {
    AccessPolicies.requireReadCabinet(actor, id);
    return findById(id);
  }

  @Transactional(readOnly = true)
  public List<MedicalOrganizationDTO> listAllOrganizations() {
    return organizationRepository.findAll(Sort.by("nom")).stream()
        .map(MedicalOrganizationDTO::fromEntity)
        .toList();
  }

  @Transactional
  public MedicalOrganizationDTO updateForActor(ProUser actor, Long id, MedicalOrganizationDTO body) {
    AccessPolicies.requirePlatformAdminOrCabinetPraticien(actor, id);
    return applyOrganizationUpdate(id, body);
  }

  private MedicalOrganizationDTO applyOrganizationUpdate(Long id, MedicalOrganizationDTO body) {
    MedicalOrganization org =
        organizationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cabinet introuvable"));

    if (body.getNom() != null && !body.getNom().isBlank()) {
      org.setNom(body.getNom().trim());
    }
    if (body.getSiret() != null) {
      org.setSiret(body.getSiret().isBlank() ? null : body.getSiret().trim());
    }
    if (body.getEmail() != null) {
      org.setEmail(body.getEmail().trim());
    }
    if (body.getTelephone() != null) {
      org.setTelephone(body.getTelephone().trim());
    }
    if (body.getAdresse() != null) {
      org.setAdresse(body.getAdresse());
    }
    if (body.getVille() != null) {
      org.setVille(body.getVille());
    }
    if (body.getCodePostal() != null) {
      org.setCodePostal(body.getCodePostal());
    }
    if (body.getPays() != null) {
      org.setPays(body.getPays());
    }
    org = organizationRepository.save(org);
    return MedicalOrganizationDTO.fromEntity(org);
  }

  @Transactional(readOnly = true)
  public List<ProUserDTO> listUsersByOrganization(Long organizationId) {
    return proUserRepository.findByOrganizationId(organizationId).stream()
        .map(ProUserDTO::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProUserDTO> listUsersForActor(ProUser actor, Long organizationId) {
    AccessPolicies.requirePlatformAdminOrCabinetPraticien(actor, organizationId);
    return listUsersByOrganization(organizationId);
  }

  /**
   * Crée un compte secondaire au sein d'un cabinet. Hors admin plateforme, seul le rôle
   * {@link ProUserRole#ASSISTANT} est autorisé.
   */
  @Transactional
  public ProUserDTO createUserInCabinet(
      ProUser actor, Long organizationId, CreateProUserRequest request) {
    AccessPolicies.requirePlatformAdminOrCabinetPraticien(actor, organizationId);
    if (request.getRole() != ProUserRole.ASSISTANT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Seul le rôle ASSISTANT peut être créé pour un compte cabinet secondaire");
    }

    MedicalOrganization org =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cabinet introuvable"));

    if (proUserRepository.existsByEmail(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
    }
    if (proUserRepository.existsByTelephone(request.getTelephone())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Téléphone déjà utilisé");
    }

    String rawPassword = request.getMotDePasse();
    if (rawPassword == null || rawPassword.isBlank()) {
      rawPassword = generateTemporaryPassword();
    }

    ProUser user = new ProUser();
    user.setOrganization(org);
    user.setEmail(request.getEmail().trim());
    user.setTelephone(request.getTelephone().trim());
    user.setPrenom(request.getPrenom().trim());
    user.setNom(request.getNom().trim());
    user.setRole(ProUserRole.ASSISTANT);
    user.setMotDePasse(passwordEncoder.encode(rawPassword));
    user.setCguAcceptees(true);
    user.setDateInscription(Instant.now());
    user = proUserRepository.save(user);

    try {
      otpService.sendOtp(user.getEmail(), "email");
    } catch (Exception e) {
      log.warn("OTP email échoué : {}", e.getMessage());
    }

    log.info("Utilisateur {} créé dans cabinet {} (rôle=ASSISTANT)", user.getId(), organizationId);
    return ProUserDTO.fromEntity(user);
  }

  @Transactional
  public void desactiverUser(ProUser actor, Long userId) {
    ProUser target =
        proUserRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    if (AccessPolicies.isPlatformAdmin(target)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite sur ce compte");
    }
    if (target.getOrganization() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite sur ce compte");
    }
    Long orgId = target.getOrganization().getId();
    AccessPolicies.requirePlatformAdminOrCabinetPraticien(actor, orgId);
    if (actor.getId().equals(target.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas désactiver votre propre compte");
    }
    target.setActif(false);
    proUserRepository.save(target);
  }

  @Transactional
  public void reactiverUser(ProUser actor, Long userId) {
    ProUser target =
        proUserRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    if (AccessPolicies.isPlatformAdmin(target)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite sur ce compte");
    }
    if (target.getOrganization() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action interdite sur ce compte");
    }
    Long orgId = target.getOrganization().getId();
    AccessPolicies.requirePlatformAdminOrCabinetPraticien(actor, orgId);
    target.setActif(true);
    proUserRepository.save(target);
  }

  @Transactional
  public void toggleUserStatus(ProUser actor, Long userId) {
    ProUser target =
        proUserRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    if (target.isActif()) {
      desactiverUser(actor, userId);
    } else {
      reactiverUser(actor, userId);
    }
  }

  private static String generateTemporaryPassword() {
    StringBuilder sb = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
      sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
