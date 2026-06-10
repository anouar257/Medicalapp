package com.medical.patient.controller;

import com.medical.patient.dto.*;
import com.medical.patient.entity.Patient;
import com.medical.patient.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrôleur d'authentification patient.
 *
 * <p>Endpoints publics (pas de JWT requis) :
 * <ul>
 *   <li>POST /api/auth/check-existence — Vérifier si email/tél existe déjà</li>
 *   <li>POST /api/auth/register — Inscription</li>
 *   <li>POST /api/auth/login — Connexion</li>
 *   <li>POST /api/auth/send-otp — Envoyer un code OTP</li>
 *   <li>POST /api/auth/verify-otp — Vérifier un code OTP</li>
 *   <li>POST /api/auth/forgot-password — Demander une réinitialisation</li>
 *   <li>POST /api/auth/reset-password — Réinitialiser le mot de passe</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String PATIENT_ID_KEY = "patientId";
    private static final String EMAIL_KEY = "email";
    private static final String TELEPHONE_KEY = "telephone";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Vérifie si un email ou un téléphone est déjà enregistré.
     */
    @PostMapping("/check-existence")
    public ResponseEntity<Map<String, Boolean>> checkExistence(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault(EMAIL_KEY, "");
        String telephone = body.getOrDefault(TELEPHONE_KEY, "");
        boolean exists = authService.existsByEmailOrTelephone(email, telephone);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Inscription d'un nouveau patient.
     * Déclenche automatiquement l'envoi d'OTP par email et SMS.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Patient patient = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    MESSAGE_KEY, "Inscription réussie. Vérifiez votre email et votre téléphone.",
                    PATIENT_ID_KEY, patient.getId(),
                    EMAIL_KEY, patient.getEmail(),
                    TELEPHONE_KEY, patient.getTelephone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Connexion — retourne un token JWT + infos patient.
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Envoie un code OTP (SMS ou Email) via Twilio Verify.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            authService.resendOtp(request);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Code OTP envoyé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Erreur lors de l'envoi du code OTP : " + e.getMessage()));
        }
    }

    /**
     * Vérifie le code OTP soumis et marque le canal comme vérifié.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VerifyOtpRequest request) {
        boolean verified = authService.verifyOtp(request);
        if (verified) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put(MESSAGE_KEY, "Vérification réussie");
            body.put("verified", true);
            authService
                    .tryRefreshJwtAfterSuccessfulOtp(authorization, request)
                    .ifPresent(
                            ar -> {
                                body.put("token", ar.getToken());
                                body.put(PATIENT_ID_KEY, ar.getPatientId());
                                body.put(EMAIL_KEY, ar.getEmail());
                                body.put("prenom", ar.getPrenom());
                                body.put("nom", ar.getNom());
                                body.put("sexe", ar.getSexe());
                                body.put("dateNaissance", ar.getDateNaissance());
                                body.put(TELEPHONE_KEY, ar.getTelephone());
                                body.put("emailVerifie", ar.isEmailVerifie());
                                body.put("telephoneVerifie", ar.isTelephoneVerifie());
                            });
            return ResponseEntity.ok(body);
        } else {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Code OTP invalide ou expiré", "verified", false));
        }
    }

    /**
     * Demande de réinitialisation du mot de passe (email ou SMS).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Instructions de réinitialisation envoyées"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    /**
     * Réinitialise le mot de passe avec le token reçu par email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Mot de passe réinitialisé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
}
