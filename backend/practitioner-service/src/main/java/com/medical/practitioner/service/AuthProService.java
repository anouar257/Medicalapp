package com.medical.practitioner.service;

import com.medical.practitioner.config.JwtUtil;
import com.medical.practitioner.dto.AuthProResponse;
import com.medical.practitioner.dto.ForgotPasswordRequest;
import com.medical.practitioner.dto.LoginProRequest;
import com.medical.practitioner.dto.RegisterCabinetRequest;
import com.medical.practitioner.dto.RegisterPractitionerRequest;
import com.medical.practitioner.dto.ResetPasswordRequest;
import com.medical.practitioner.dto.SendOtpRequest;
import com.medical.practitioner.dto.VerifyOtpRequest;
import com.medical.practitioner.entity.MedicalOrganization;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.entity.Specialty;
import com.medical.practitioner.entity.StatutPraticien;
import com.medical.practitioner.entity.Titre;
import com.medical.practitioner.repository.MedicalOrganizationRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.repository.ProUserRepository;
import com.medical.practitioner.repository.SpecialtyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service d'authentification professionnel.
 *
 * <p>Gère :
 * <ul>
 *   <li>l'inscription d'un cabinet (création MedicalOrganization + ProUser PRATICIEN titulaire + profil) ;</li>
 *   <li>l'inscription self-service d'un praticien (création ProUser PRATICIEN + PractitionerProfile) ;</li>
 *   <li>la connexion (lecture du rôle en base, retour dans le JWT — aucun choix de rôle côté client) ;</li>
 *   <li>les OTP email/SMS via Twilio Verify ;</li>
 *   <li>la réinitialisation du mot de passe par email (lien) ou SMS (code).</li>
 * </ul>
 */
@Service
public class AuthProService {

    private static final Logger log = LoggerFactory.getLogger(AuthProService.class);
    private static final String CHANNEL_EMAIL = "email";

    private final ProUserRepository proUserRepository;
    private final MedicalOrganizationRepository organizationRepository;
    private final PractitionerProfileRepository practitionerProfileRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final JavaMailSender mailSender;
    private final AgendaSyncService agendaSyncService;

    @Value("${app.password-reset.token-expiration-minutes}")
    private int resetTokenExpirationMinutes;

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

    @Value("${spring.mail.username:noreply@mediconnect.com}")
    private String fromEmail;

    public AuthProService(ProUserRepository proUserRepository,
                          MedicalOrganizationRepository organizationRepository,
                          PractitionerProfileRepository practitionerProfileRepository,
                          SpecialtyRepository specialtyRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          OtpService otpService,
                          JavaMailSender mailSender,
                          AgendaSyncService agendaSyncService) {
        this.proUserRepository = proUserRepository;
        this.organizationRepository = organizationRepository;
        this.practitionerProfileRepository = practitionerProfileRepository;
        this.specialtyRepository = specialtyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.mailSender = mailSender;
        this.agendaSyncService = agendaSyncService;
    }

    // ── Vérification d'existence ─────────────────────────────────────────────

    public boolean existsByEmailOrTelephone(String email, String telephone) {
        boolean emailExists = email != null && !email.isBlank() && proUserRepository.existsByEmail(email);
        boolean telExists = telephone != null && !telephone.isBlank() && proUserRepository.existsByTelephone(telephone);
        return emailExists || telExists;
    }

    // ── Inscription Cabinet ─────────────────────────────────────────────────

    @Transactional
    public ProUser registerCabinet(RegisterCabinetRequest request) {
        if (proUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }
        if (proUserRepository.existsByTelephone(request.getTelephone())) {
            throw new IllegalArgumentException("Un compte avec ce téléphone existe déjà");
        }

        MedicalOrganization org = new MedicalOrganization();
        org.setNom(request.getNomCabinet());
        org.setSiret(request.getSiret());
        org.setAdresse(request.getAdresseCabinet());
        org.setVille(request.getVilleCabinet());
        org.setCodePostal(request.getCodePostalCabinet());
        org.setTelephone(request.getTelephoneCabinet());
        org.setEmail(request.getEmail());
        org = organizationRepository.save(org);

        ProUser owner = new ProUser();
        owner.setOrganization(org);
        owner.setEmail(request.getEmail());
        owner.setTelephone(request.getTelephone());
        owner.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        owner.setRole(ProUserRole.PRATICIEN);
        owner.setPrenom(request.getPrenom());
        owner.setNom(request.getNom());
        owner.setCguAcceptees(true);
        owner.setDateInscription(Instant.now());
        ProUser saved = proUserRepository.save(owner);

        PractitionerProfile profile = new PractitionerProfile();
        profile.setProUser(saved);
        profile.setTitre(Titre.AUCUN);
        profile.setStatut(StatutPraticien.TITULAIRE);
        profile.setEmpreinte(generateEmpreinte(saved.getId()));
        Optional<Specialty> defaultSpecialty = specialtyRepository.findByCode("MEDECINE_GENERALE");
        if (defaultSpecialty.isPresent()) {
            Set<Specialty> set = new HashSet<>();
            set.add(defaultSpecialty.get());
            profile.setSpecialites(set);
        }
        profile = practitionerProfileRepository.save(profile);
        saved.setPractitionerProfile(profile);

        agendaSyncService.syncPractitioner(profile);

        log.info("Cabinet inscrit : org={} praticien-responsable={}", org.getId(), saved.getId());

        sendOtpsBestEffort(saved);
        return saved;
    }

    // ── Inscription Praticien (self-service) ────────────────────────────────

    @Transactional
    public PractitionerProfile registerPractitioner(RegisterPractitionerRequest request) {
        if (proUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }
        if (proUserRepository.existsByTelephone(request.getTelephone())) {
            throw new IllegalArgumentException("Un compte avec ce téléphone existe déjà");
        }

        MedicalOrganization org;
        if (request.getOrganizationId() != null) {
            org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Cabinet introuvable"));
        } else {
            org = new MedicalOrganization();
            String nomCabinet = request.getNomCabinetPersonnel();
            if (nomCabinet == null || nomCabinet.isBlank()) {
                nomCabinet = "Cabinet de " + request.getPrenom() + " " + request.getNom();
            }
            org.setNom(nomCabinet);
            org.setEmail(request.getEmail());
            org.setTelephone(request.getTelephone());
            org = organizationRepository.save(org);
        }

        ProUser user = new ProUser();
        user.setOrganization(org);
        user.setEmail(request.getEmail());
        user.setTelephone(request.getTelephone());
        user.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        user.setRole(ProUserRole.PRATICIEN);
        user.setPrenom(request.getPrenom());
        user.setNom(request.getNom());
        user.setCguAcceptees(true);
        user.setDateInscription(Instant.now());
        user = proUserRepository.save(user);

        PractitionerProfile profile = new PractitionerProfile();
        profile.setProUser(user);
        profile.setCivilite(request.getCivilite());
        profile.setTitre(request.getTitre() != null ? request.getTitre() : com.medical.practitioner.entity.Titre.AUCUN);
        profile.setSexe(request.getSexe());
        profile.setDateNaissance(request.getDateNaissance());
        profile.setStatut(request.getStatut() != null ? request.getStatut() : com.medical.practitioner.entity.StatutPraticien.TITULAIRE);
        profile.setEmpreinte(generateEmpreinte(user.getId()));

        if (request.getSpecialiteIds() != null && !request.getSpecialiteIds().isEmpty()) {
            List<Specialty> found = specialtyRepository.findAllById(request.getSpecialiteIds());
            profile.setSpecialites(new HashSet<>(found));
        } else {
            Optional<Specialty> defaultSpecialty = specialtyRepository.findByCode("MEDECINE_GENERALE");
            if (defaultSpecialty.isPresent()) {
                Set<Specialty> set = new HashSet<>();
                set.add(defaultSpecialty.get());
                profile.setSpecialites(set);
            }
        }

        profile = practitionerProfileRepository.save(profile);
        user.setPractitionerProfile(profile);

        log.info("Praticien inscrit : userId={} profileId={} org={}",
                user.getId(), profile.getId(), org.getId());

        agendaSyncService.syncPractitioner(profile);

        sendOtpsBestEffort(user);
        return profile;
    }

    private String generateEmpreinte(Long userId) {
        return "PRAT-" + String.format("%08d", userId) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void sendOtpsBestEffort(ProUser user) {
        try {
            otpService.sendOtp(user.getEmail(), CHANNEL_EMAIL);
        } catch (Exception e) {
            log.warn("OTP email échoué : {}", e.getMessage());
        }
        try {
            otpService.sendOtp(user.getTelephone(), "sms");
        } catch (Exception e) {
            log.warn("OTP sms échoué : {}", e.getMessage());
        }
    }

    // ── OTP ─────────────────────────────────────────────────────────────────

    public void resendOtp(SendOtpRequest req) {
        otpService.sendOtp(req.getTo(), req.getChannel());
    }

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest req) {
        boolean ok = otpService.verifyOtp(req.getTo(), req.getCode());
        if (!ok) return false;

        if (CHANNEL_EMAIL.equalsIgnoreCase(req.getChannel())) {
            proUserRepository.findByEmail(req.getTo()).ifPresent(u -> {
                u.setEmailVerifie(true);
                proUserRepository.save(u);
            });
        } else if ("sms".equalsIgnoreCase(req.getChannel())) {
            proUserRepository.findByTelephone(req.getTo()).ifPresent(u -> {
                u.setTelephoneVerifie(true);
                proUserRepository.save(u);
            });
        }
        return true;
    }

    /**
     * Si un Bearer pro valide correspond au canal OTP vérifié, renvoie une session avec JWT à jour
     * (claim {@code isVerified}).
     */
    public Optional<AuthProResponse> tryRefreshJwtAfterSuccessfulOtp(
            String authorizationHeader, VerifyOtpRequest request) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return Optional.empty();
        }
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return Optional.empty();
        }
        ProUser user = proUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        if (CHANNEL_EMAIL.equalsIgnoreCase(request.getChannel())) {
            if (!user.getEmail().equalsIgnoreCase(request.getTo())) {
                return Optional.empty();
            }
        } else if ("sms".equalsIgnoreCase(request.getChannel())) {
            if (!user.getTelephone().equals(request.getTo())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
        ProUser fresh = proUserRepository.findById(userId).orElseThrow();
        Long orgId = fresh.getOrganization() != null ? fresh.getOrganization().getId() : null;
        Long practitionerProfileId =
                practitionerProfileRepository
                        .findByProUserId(fresh.getId())
                        .map(PractitionerProfile::getId)
                        .orElse(null);
        String newToken =
                jwtUtil.generateToken(
                        fresh.getEmail(),
                        fresh.getId(),
                        fresh.getRole(),
                        orgId,
                        practitionerProfileId,
                        fresh.isEmailVerifie());
        return Optional.of(buildAuthProResponse(fresh, newToken, orgId, practitionerProfileId));
    }

    // ── Login (retour du rôle exact) ────────────────────────────────────────

    public AuthProResponse login(LoginProRequest req) {
        Optional<ProUser> userOpt = proUserRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            userOpt = proUserRepository.findByTelephone(req.getEmail());
        }

        ProUser user = userOpt.orElseThrow(() ->
                new IllegalArgumentException("Identifiant ou mot de passe incorrect"));

        if (!user.isActif()) {
            throw new IllegalArgumentException("Ce compte a été désactivé");
        }
        if (!passwordEncoder.matches(req.getMotDePasse(), user.getMotDePasse())) {
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
        }

        if (user.getRole() == ProUserRole.ADMIN && user.getOrganization() != null) {
            throw new IllegalStateException("Configuration du compte administrateur plateforme invalide");
        }

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        Long practitionerProfileId =
                practitionerProfileRepository.findByProUserId(user.getId())
                        .map(p -> p.getId())
                        .orElse(null);
        String token =
                jwtUtil.generateToken(
                        user.getEmail(),
                        user.getId(),
                        user.getRole(),
                        orgId,
                        practitionerProfileId,
                        user.isEmailVerifie());

        log.info("Login pro réussi : userId={} role={}", user.getId(), user.getRole());
        return buildAuthProResponse(user, token, orgId, practitionerProfileId);
    }

    private AuthProResponse buildAuthProResponse(
            ProUser user, String token, Long organizationId, Long practitionerProfileId) {
        AuthProResponse resp = new AuthProResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setTelephone(user.getTelephone());
        resp.setPrenom(user.getPrenom());
        resp.setNom(user.getNom());
        resp.setRole(user.getRole());
        resp.setOrganizationId(organizationId);
        if (user.getOrganization() != null) {
            resp.setOrganizationNom(user.getOrganization().getNom());
        }
        resp.setEmailVerifie(user.isEmailVerifie());
        resp.setTelephoneVerifie(user.isTelephoneVerifie());

        if (practitionerProfileId != null) {
            resp.setPractitionerProfileId(practitionerProfileId);
        }
        return resp;
    }

    // ── Mot de passe oublié ─────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        Optional<ProUser> userOpt;
        if (CHANNEL_EMAIL.equalsIgnoreCase(req.getChannel())) {
            userOpt = proUserRepository.findByEmail(req.getIdentifiant());
        } else {
            userOpt = proUserRepository.findByTelephone(req.getIdentifiant());
        }
        ProUser user = userOpt.orElseThrow(() ->
                new IllegalArgumentException("Aucun compte trouvé avec cet identifiant"));
        if (CHANNEL_EMAIL.equalsIgnoreCase(req.getChannel())) {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(Instant.now().plus(resetTokenExpirationMinutes, ChronoUnit.MINUTES));
            proUserRepository.save(user);
            log.info("[PasswordReset] Token de réinitialisation généré pour {}", user.getEmail());
            sendResetEmail(user.getEmail(), token);
        } else {
            otpService.sendOtp(user.getTelephone(), "sms");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        ProUser user = proUserRepository.findByResetToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Le token a expiré");
        }
        user.setMotDePasse(passwordEncoder.encode(req.getNouveauMotDePasse()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        proUserRepository.save(user);
    }

    private void sendResetEmail(String to, String token) {
        String link = resetBaseUrl + "?token=" + token;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject("Mediconnect Pro — Réinitialisation de votre mot de passe");
            msg.setText("""
                    Bonjour,

                    Vous avez demandé la réinitialisation du mot de passe de votre compte professionnel.

                    Cliquez sur le lien ci-dessous pour définir un nouveau mot de passe :
                    """ + link + """


                    Ce lien expire dans """ + resetTokenExpirationMinutes + """
                     minutes.

                    Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.

                    L'équipe Mediconnect Pro
                    """);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Email de reset non envoyé : {}", e.getMessage());
        }
    }
}
