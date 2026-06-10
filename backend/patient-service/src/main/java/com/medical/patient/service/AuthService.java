package com.medical.patient.service;

import com.medical.patient.config.JwtUtil;
import com.medical.patient.dto.*;
import com.medical.patient.entity.Patient;
import com.medical.patient.repository.PatientRepository;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Service d'authentification patient — inscription, login, OTP, reset password.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String CHANNEL_EMAIL = "email";

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final JavaMailSender mailSender;

    @Value("${app.password-reset.token-expiration-minutes}")
    private int resetTokenExpirationMinutes;

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

    @Value("${spring.mail.username:noreply@mediconnect.com}")
    private String fromEmail;

    public AuthService(PatientRepository patientRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       JavaMailSender mailSender) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.mailSender = mailSender;
    }

    // ── Vérification existence ───────────────────────────────────────────────

    /**
     * Vérifie si un email ou un téléphone est déjà enregistré.
     */
    public boolean existsByEmailOrTelephone(String email, String telephone) {
        return patientRepository.existsByEmail(email) || patientRepository.existsByTelephone(telephone);
    }

    // ── Inscription ──────────────────────────────────────────────────────────

    /**
     * Inscrit un nouveau patient.
     * Le mot de passe est hashé en BCrypt.
     * Les vérifications email/téléphone sont gérées séparément via OTP.
     */
    @Transactional
    public Patient register(RegisterRequest request) {
        // Vérifier l'unicité
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }
        if (patientRepository.existsByTelephone(request.getTelephone())) {
            throw new IllegalArgumentException("Un compte avec ce numéro de téléphone existe déjà");
        }
        if (!Boolean.TRUE.equals(request.getCguAcceptees())) {
            throw new IllegalArgumentException("Vous devez accepter les conditions générales d'utilisation");
        }

        Patient patient = new Patient();
        patient.setSexe(request.getSexe());
        patient.setPrenom(request.getPrenom());
        patient.setNom(request.getNom());
        patient.setDateNaissance(request.getDateNaissance());
        patient.setEmail(request.getEmail());
        patient.setTelephone(request.getTelephone());
        patient.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        patient.setCguAcceptees(true);
        patient.setDateInscription(Instant.now());
        patient.setVille(request.getVille());

        Patient saved = patientRepository.save(patient);
        log.info("Patient inscrit avec succès — ID: {}, Email: {}", saved.getId(), saved.getEmail());

        // Envoyer OTP par email et SMS pour vérification
        try {
            otpService.sendOtp(saved.getEmail(), CHANNEL_EMAIL);
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'OTP email à {} : {}", saved.getEmail(), e.getMessage());
        }
        try {
            otpService.sendOtp(saved.getTelephone(), "sms");
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'OTP SMS à {} : {}", saved.getTelephone(), e.getMessage());
        }

        return saved;
    }

    // ── Vérification OTP ─────────────────────────────────────────────────────

    /**
     * Vérifie le code OTP et marque le canal comme vérifié.
     */
    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        boolean verified = otpService.verifyOtp(request.getTo(), request.getCode());

        if (verified) {
            if (CHANNEL_EMAIL.equalsIgnoreCase(request.getChannel())) {
                Optional<Patient> patientOpt = patientRepository.findByEmail(request.getTo());
                patientOpt.ifPresent(p -> {
                    p.setEmailVerifie(true);
                    patientRepository.save(p);
                    log.info("Email vérifié pour le patient ID: {}", p.getId());
                });
            } else if ("sms".equalsIgnoreCase(request.getChannel())) {
                Optional<Patient> patientOpt = patientRepository.findByTelephone(request.getTo());
                patientOpt.ifPresent(p -> {
                    p.setTelephoneVerifie(true);
                    patientRepository.save(p);
                    log.info("Téléphone vérifié pour le patient ID: {}", p.getId());
                });
            }
        }

        return verified;
    }

    /**
     * Si un Bearer patient valide correspond au canal OTP vérifié, renvoie un nouveau JWT (claim
     * {@code isVerified} à jour).
     */
    public Optional<AuthResponse> tryRefreshJwtAfterSuccessfulOtp(
            String authorizationHeader, VerifyOtpRequest request) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return Optional.empty();
        }
        Long patientId = jwtUtil.extractPatientId(token);
        if (patientId == null) {
            return Optional.empty();
        }
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null) {
            return Optional.empty();
        }
        if (CHANNEL_EMAIL.equalsIgnoreCase(request.getChannel())) {
            if (!patient.getEmail().equalsIgnoreCase(request.getTo())) {
                return Optional.empty();
            }
        } else if ("sms".equalsIgnoreCase(request.getChannel())) {
            if (!patient.getTelephone().equals(request.getTo())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
        String newToken = jwtUtil.generateToken(patient.getEmail(), patient.getId(), patient.isEmailVerifie());
        return Optional.of(toAuthResponse(patient, newToken));
    }

    private AuthResponse toAuthResponse(Patient patient, String token) {
        return AuthResponse.builder()
                .token(token)
                .patientId(patient.getId())
                .email(patient.getEmail())
                .prenom(patient.getPrenom())
                .nom(patient.getNom())
                .sexe(patient.getSexe())
                .dateNaissance(patient.getDateNaissance())
                .telephone(patient.getTelephone())
                .emailVerifie(patient.isEmailVerifie())
                .telephoneVerifie(patient.isTelephoneVerifie())
                .ville(patient.getVille())
                .build();
    }

    /**
     * Renvoie un code OTP au patient.
     */
    public void resendOtp(SendOtpRequest request) {
        otpService.sendOtp(request.getTo(), request.getChannel());
    }

    // ── Connexion ────────────────────────────────────────────────────────────

    /**
     * Authentifie un patient par email ou téléphone + mot de passe.
     * Retourne un token JWT + les infos du patient.
     */
    public AuthResponse login(LoginRequest request) {
        // Chercher par email ou par téléphone
        Optional<Patient> patientOpt = patientRepository.findByEmail(request.getIdentifiant());
        if (patientOpt.isEmpty()) {
            patientOpt = patientRepository.findByTelephone(request.getIdentifiant());
        }

        Patient patient = patientOpt
                .orElseThrow(() -> new IllegalArgumentException("Identifiant ou mot de passe incorrect"));

        if (!patient.isActif()) {
            throw new IllegalArgumentException("Ce compte a été désactivé");
        }

        if (!passwordEncoder.matches(request.getMotDePasse(), patient.getMotDePasse())) {
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect");
        }

        String token = jwtUtil.generateToken(patient.getEmail(), patient.getId(), patient.isEmailVerifie());

        log.info("Connexion réussie — Patient ID: {}", patient.getId());

        return toAuthResponse(patient, token);
    }

    // ── Mot de passe oublié ──────────────────────────────────────────────────

    /**
     * Gère la demande de réinitialisation du mot de passe.
     * Envoie un lien de réinitialisation par email ou un code OTP par SMS.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<Patient> patientOpt;

        if (CHANNEL_EMAIL.equalsIgnoreCase(request.getChannel())) {
            patientOpt = patientRepository.findByEmail(request.getIdentifiant());
        } else {
            patientOpt = patientRepository.findByTelephone(request.getIdentifiant());
        }

        Patient patient = patientOpt
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé avec cet identifiant"));

        if (CHANNEL_EMAIL.equalsIgnoreCase(request.getChannel())) {
            // Générer un token unique et envoyer par email
            String resetToken = UUID.randomUUID().toString();
            patient.setResetToken(resetToken);
            patient.setResetTokenExpiry(Instant.now().plus(resetTokenExpirationMinutes, ChronoUnit.MINUTES));
            patientRepository.save(patient);

            sendResetEmail(patient.getEmail(), resetToken);
            log.info("Lien de réinitialisation envoyé par email au patient ID: {}", patient.getId());
        } else {
            // Envoyer un OTP par SMS via Twilio
            otpService.sendOtp(patient.getTelephone(), "sms");
            log.info("Code de réinitialisation envoyé par SMS au patient ID: {}", patient.getId());
        }
    }

    /**
     * Réinitialise le mot de passe avec le token reçu par email.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Patient patient = patientRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de réinitialisation invalide"));

        if (patient.getResetTokenExpiry() == null || patient.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Le token de réinitialisation a expiré");
        }

        patient.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        patient.setResetToken(null);
        patient.setResetTokenExpiry(null);
        patientRepository.save(patient);

        log.info("Mot de passe réinitialisé avec succès pour le patient ID: {}", patient.getId());
    }

    // ── Utilitaire email ─────────────────────────────────────────────────────

    private void sendResetEmail(String to, String resetToken) {
        String resetLink = resetBaseUrl + "?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Mediconnect — Réinitialisation de votre mot de passe");
        message.setText(
            "Bonjour,\n\n" +
            "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
            "Cliquez sur le lien suivant pour définir un nouveau mot de passe :\n" +
            resetLink + "\n\n" +
            "Ce lien expire dans " + resetTokenExpirationMinutes + " minutes.\n\n" +
            "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
            "L'équipe Mediconnect"
        );

        mailSender.send(message);
    }
}
